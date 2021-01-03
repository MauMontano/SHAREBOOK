/*
 * Copyright (c) 2021. Benjamín Antonio Velasco Guzmán
 * Author: Benjamín Antonio Velasco Guzmán <9benjaminguzman@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.fos;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.management.InstanceAlreadyExistsException;
import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import org.jetbrains.annotations.NotNull;

public class ChatServer implements Runnable {
	private static boolean alreadyInstantiated;

	private final ServerSocket serverSocket;
	private final ConcurrentHashMap<ChatUser, ChatSocket> connectedClients; // maps user object -> chat socket
	private final ConcurrentHashMap<Integer, ChatUser> connectedUsers; // maps user_chat_id -> user object

	public ChatServer() throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException, UnrecoverableKeyException, KeyManagementException, InstanceAlreadyExistsException {
		if (alreadyInstantiated)
			throw new InstanceAlreadyExistsException("You cannot instantiate ChatServer more than once");
		alreadyInstantiated = true;

		Properties config = new Properties();
		config.load(this.getClass().getResourceAsStream("/resources/config.properties"));

		int server_port = Integer.parseInt(config.getProperty("CHAT_PORT"));

		this.serverSocket = getServerSocketFactory().createServerSocket(server_port);
		this.connectedClients = new ConcurrentHashMap<>(3);
		this.connectedUsers = new ConcurrentHashMap<>(3);
		System.out.println("Socket server created and ready to accept connections on port: " + server_port);
	}

	/**
	 * configures the {@link ServerSocketFactory} with the private key
	 */
	private static ServerSocketFactory getServerSocketFactory() throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, CertificateException, KeyManagementException, IOException, UnrecoverableKeyException {
		// password for the private key
		char[] password = "sharebook".toCharArray();

		// load the keystore containing the private key and cert file
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(ChatServer.class.getResourceAsStream("/resources/secrets/keystore.jks"), password);

		// initialize the key manager to use within the ssl context
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
		keyManagerFactory.init(keyStore, password);

		SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
		sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

		return sslContext.getServerSocketFactory();
	}

	/**
	 * Waits for a client to connect, this will automatically start a new thread so other clients can connect
	 */
	@Override
	public void run() {
		String clientAddr = null;
		try (Socket clientSocket = this.serverSocket.accept()) {
			// start a new thread to accept a new client
			// this is sort of a recursive way to do a loop and wait for connections
			this.newListener();

			clientSocket.setSoTimeout(60_000 * 30 /* 1m -> 30m */ ); // timeout after 30 minutes of inactivity
			clientAddr = clientSocket.getInetAddress().toString();

			ChatSocket clientChatSocket = new ChatSocket(clientSocket);

			// handle the new connection
			if (!this.handleNewClientConnected(clientChatSocket)) {
				// if the user couldn't be authenticated, close the connection
				// the user should try again by opening a new connection
				clientChatSocket.close();
				return;
			}

			// if everything went good, start receiving and sending messages
			while (!clientChatSocket.isClosed())
				this.handleClientRequest(clientChatSocket);
		} catch (SocketTimeoutException e) {
			System.out.println("The socket from " + clientAddr + " has timed out. Connection was closed");
			e.printStackTrace();
			// TODO: REMOVE CLIENT SOCKET FROM THE HASHMAP
		} catch(InvalidClientRequest e) {
			System.err.println("The client from " + clientAddr + " sent and invalid request!!");
			e.printStackTrace();
		} catch (SSLException e) {
			e.printStackTrace();
			System.err.println("Probably the above exception occurred because someone tried to connect " +
				"trough a non-SSL channel (e. g. HTTP instead of HTTPS)");
			System.out.println("If this were a web server, an HTTP server should be listening and " +
				"redirecting to the HTTPS server");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Starts a new thread from this Runnable class to listen for new incoming connections
	 *
	 * The {@link #run()} method will automatically invoke this method again so there is no need to invoke it again
	 * from outside this class
	 */
	public void newListener() {
		Thread listenerThread = new Thread(this);
		listenerThread.start();
	}

	/**
	 * Handles the new client connected
	 *
	 * This method will first read the headers and if valid, the clientSocket will be added to the
	 * {@link #connectedClients} hashmap
	 *
	 * This will query the authentication microservice to know if the authHash is valid and is therefore a trusted user
	 * the autHash is obtained from within the socketInputBuff, the client should send it
	 *
	 * Once the authHash is obtained, query the authentication microservice to know if it is a valid hash
	 * and also obtain the corresponding hashcode for the user
	 * that hashcode will be used to identify each socket (each user) in this chat
	 * The microservice will also return the username for the user and maybe some other info @MauSwoosh decide it
	 * @param clientSocket the client socket
	 * @return true if the method could authenticate correctly the user, false otherwise. IF the user couldn't be
	 * authenticated, the socket will not be added to the {@link #connectedClients} hashmap
	 * and a message indicating the authentication failed will be send trough it
	 */
	private boolean handleNewClientConnected(@NotNull ChatSocket clientSocket) throws IOException {
		// first line should contain the request type header
		String reqTypeHeader = clientSocket.readLine();

		ClientRequestType reqType = ClientRequestType.fromHeader(reqTypeHeader);
		if (reqType != ClientRequestType.CONNECT)
			throw new InvalidClientRequest("First request sent from the client SHOULD be a connect request");

		// read next line that should contain the authentication hash
		char[] authHash = clientSocket.readLine().toCharArray();

		// TODO: QUERY THE AUTH MICROSERVICE TO KNOW IF THE AUTH HASH IS GOOD

		// FIXME: remove this "vulnerability"
		if ("loginhashbypasss".equals(String.valueOf(authHash))) {
			ChatUser connectedUser = new ChatUser(10, "el mau");

			// notify all other connected clients a new client has connected
			this.broadCastUserConnected(connectedUser);
			this.connectedClients.put(connectedUser, clientSocket);
			this.connectedUsers.put(connectedUser.getHashCode(), connectedUser);

			// add hook to remove the socket when it is closed
			clientSocket.setOnCloseHook(() -> {
				this.connectedClients.remove(connectedUser);
				this.connectedUsers.remove(connectedUser.getHashCode());
			});

			// notify the user everything went OK
			clientSocket.writeLines(
				ServerResponseType.CONNECTION_SUCCESS.getHeader(),
				String.valueOf(connectedUser.getHashCode())
			);

			return true;
		} else { // the user couldn't be authenticated
			clientSocket.writeLines(
				ServerResponseType.CONNECTION_FAILED.getHeader(), // write header
				ServerResponseFailedReason.UNAUTHORIZED.getReason() // write reason
			);
		}
		return false;
	}

	/**
	 * this method will handle {@link ClientRequestType#MESSAGE} or {@link ClientRequestType#LOGOUT}
	 * events
	 * @param clientSocket the client socket
	 */
	private void handleClientRequest(@NotNull ChatSocket clientSocket) throws IOException {
		String reqTypeHeader = clientSocket.readLine();

		ClientRequestType reqType = ClientRequestType.fromHeader(reqTypeHeader);
		if (reqType == ClientRequestType.LOGOUT) {
			// close the socket, if the close hook is configured,
			// the socket will be automatically removed from the hashmap
			clientSocket.close();
		} else if (reqType == ClientRequestType.MESSAGE) {
			// handle the message

			// read from ID
			String fromID = clientSocket.readLine();

			// read to ID
			String toID = clientSocket.readLine();
			int to_id = Integer.parseInt(toID);

			// read beginning message header
			String messageHeader = clientSocket.readLine(); // just ignore that header

			// read the actual message, it should be base64-encoded
			// and should be decoded in the receiving client
			String message = clientSocket.readLine();

			// read the message footer
			String messageFooter = clientSocket.readLine(); // just ignore the footer

			// forward data to the right client
			// get the right client
			ChatUser receiverUser = this.connectedUsers.get(to_id);
			this.connectedClients.get(receiverUser).writeLines(
				ServerResponseType.MESSAGE.getHeader(), // message header
				fromID, // sender ID
				toID, // receiver ID
				messageHeader,
				message, // this should be Base64-encoded
				messageFooter
			);

			System.out.println(
				"Message forwarded from "
					+ this.connectedUsers.get(Integer.parseInt(fromID))
					+ " to "
					+ receiverUser
					+ " message: "
					+ new String(
						Base64.getDecoder().decode(message.getBytes(StandardCharsets.UTF_8)),
						StandardCharsets.UTF_8
					)
			);
		} else
			throw new InvalidClientRequest(reqTypeHeader + " is an invalid header!!");
	}

	/**
	 * Broadcasts a {@link ServerResponseType#USER_CONNECTED} event to all connected sockets
	 * @param newConnectedUser the object for the new connected user
	 */
	private void broadCastUserConnected(@NotNull ChatUser newConnectedUser) {
		this.connectedClients.keySet().forEach(chatUser -> {
			try {
				ChatSocket socket = this.connectedClients.get(chatUser);
				if (socket.isClosed()) {
					this.connectedClients.remove(chatUser);
					return;
				}

				socket.writeLines(
					// write header
					ServerResponseType.USER_CONNECTED.getHeader(),

					// write connected user information
					String.valueOf(newConnectedUser.getHashCode()),
					newConnectedUser.getUsername()
				);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
}

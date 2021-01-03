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

package org.fos.chat;

import org.fos.ShareBook;
import org.jetbrains.annotations.NotNull;

import javax.management.InstanceAlreadyExistsException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.Objects;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ChatClient extends Thread {
	private static ChatClient instance;

	private ChatSocket socket;

	private final char[] loginHash;
	private int chat_uid;

	// callbacks
	// first param is the id from the sender, second param is the actual message
	@NotNull
	private BiConsumer<Integer, String> onMessage;
	@NotNull
	private final Consumer<String> onError;
	@NotNull
	private Consumer<ChatUser> onUserConnected;
	@NotNull
	private Consumer<String> onConnectionFailed;
	@NotNull
	private Runnable onConnectionSuccess;

	/**
	 * Creates a new chat client
	 *
	 * @param loginHash the hash required to start the own handshake with the chat server
	 *                  this is required so the server can tell who is talking to
	 * @throws InstanceAlreadyExistsException if this class was already instantiated
	 */
	public ChatClient(
		final char[] loginHash,
		@NotNull BiConsumer<Integer, String> onMessage,
		@NotNull Consumer<ChatUser> onUserConnected,
		@NotNull Consumer<String> onError,
		@NotNull Runnable onConnectionSuccess,
		@NotNull Consumer<String> onConnectionFailed
	) throws InstanceAlreadyExistsException {
		this.loginHash = Objects.requireNonNull(loginHash);
		this.onMessage = Objects.requireNonNull(onMessage);
		this.onUserConnected = Objects.requireNonNull(onUserConnected);
		this.onConnectionSuccess = Objects.requireNonNull(onConnectionSuccess);
		this.onConnectionFailed = Objects.requireNonNull(onConnectionFailed);
		this.onError = onError;

		this.setDaemon(true); // the client thread SHOULD be a daemon thread, NOT a user thread

		if (ChatClient.instance != null)
			throw new InstanceAlreadyExistsException();

		ChatClient.instance = this;
	}

	/**
	 * Initialize the attributes for this class
	 * @throws IOException see {@link #configuredSSLContext(String)}
	 * @throws CertificateException see {@link #configuredSSLContext(String)}
	 * @throws NoSuchAlgorithmException see {@link #configuredSSLContext(String)}
	 * @throws KeyStoreException see {@link #configuredSSLContext(String)}
	 * @throws KeyManagementException see {@link #configuredSSLContext(String)}
	 */
	private void init() throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		if (this.isInterrupted())
			return;

		Properties appConfig = ShareBook.getAppConfig();

		SSLSocketFactory factory = this.configuredSSLContext("/resources/cert.pem").getSocketFactory();
		SSLSocket socket = (SSLSocket) factory.createSocket(
			appConfig.getProperty("CHAT_IP", "127.0.0.1"),
			Integer.parseInt(appConfig.getProperty("CHAT_PORT", "12365"))
		);
		socket.startHandshake();

		this.socket = new ChatSocket(socket);
	}

	/**
	 * Configures the SSL context to accept a self-signed certificate
	 *
	 * @return an SSLContext object that will trust the given certificate
	 * @throws CertificateException     when the certificate X.509 is not supported
	 * @throws KeyStoreException        if there is no KeyStore (this exception is unlikely to happen)
	 * @throws IOException              if there was an error while reading the certificate
	 * @throws NoSuchAlgorithmException if the TLSv1.2 is not supported
	 * @throws KeyManagementException   if the operation (forcing the created ssl context to trust the self-signed cert) fails
	 */
	private SSLContext configuredSSLContext(@NotNull String certFilePath) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, KeyManagementException {
		Certificate certificate = CertificateFactory
			.getInstance("X.509")
			.generateCertificate(ChatClient.class.getResourceAsStream(certFilePath));

		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(null, null);
		keyStore.setCertificateEntry("server", certificate);

		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
			TrustManagerFactory.getDefaultAlgorithm()
		);
		trustManagerFactory.init(keyStore);

		SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
		sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

		return sslContext;
	}

	/**
	 * Sends the connect request and waits for a response from the server
	 * The response from the server will contain the {@link #chat_uid}, this method will initialize that value too
	 *
	 * @return true if the response from the server was good and the client connected successfully, false otherwise
	 * if it is false, you'll probably want to check the next line in the buffer to see why the connection failed
	 * @throws IOException if there was an error while writing to or reading from the socket
	 */
	private boolean sendConnectRequest() throws IOException {
		if (this.isInterrupted())
			return false;

		// send connect request to the chat server, this will start our own handshake with the server

		this.socket.writeLines(
			"CONNECT", // write CONNECT header
			String.valueOf(this.loginHash) // write authentication hash
		);

		// test if the connection was successful
		if (!ServerResponseType.CONNECTION_SUCCESS.getHeader().equalsIgnoreCase(this.socket.readLine()))
			return false;

		// if it was successful the server should have sent the chat id
		this.chat_uid = Integer.parseInt(this.socket.readLine());
		return true;
	}

	/**
	 * Use this method to simply send a message tto someone
	 * <p>
	 * This method is synchronized to avoid multiple threads sending a message at one time
	 *
	 * @param message the message you want to send
	 * @throws InterruptedException if the thread is interrupted
	 */
	synchronized public void sendMessage(String message, int to_id) throws InterruptedException {
		if (this.isInterrupted())
			throw new InterruptedException("The chat thread is interrupted");

		try {
			this.socket.writeLines(
				"MESSAGE", // write request header
				String.valueOf(this.chat_uid), // write FROM_ID
				String.valueOf(to_id), // write TO_ID
				"---BEGIN MESSAGE---",
				Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8)), // write message
				"---END MESSAGE---"
			);
		} catch (IOException e) {
			e.printStackTrace();
			this.onError.accept(ShareBook.getMessagesBundle().getString("chat_send_msg_error"));
		}
	}

	/**
	 * Use it to logout from the Chat server
	 */
	synchronized public void logout() throws IOException {
		this.socket.writeLines("LOGOUT");
		this.socket.close();
	}

	@Override
	public void interrupt() {
		super.interrupt();
		try {
			this.logout();
		} catch (IOException e) {
			e.printStackTrace();
		}
		ChatClient.instance = null;
	}

	@Override
	public void run() {
		// start TCP connection to the server
		try {
			this.init();
		} catch (KeyManagementException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
			// TODO: HANDLE EXCEPTION
			// exceptions are thrown if there is an error with the JVM
			// e. g. it doesn't support TLS, don't have compatible algorithms with the server, etc...
			e.printStackTrace();
			this.onError.accept(ShareBook.getMessagesBundle().getString("chat_init_error"));
			return;
		} catch (IOException e) {
			// TODO: HANDLE EXCEPTION
			// exception is likely to happen if there is an error with the certificate (unlikely)
			// or with the connectivity, e. g. if the server is DOWN
			e.printStackTrace();
			this.onError.accept(ShareBook.getMessagesBundle().getString("chat_conn_error"));
			return;
		}

		// start own-protocol connection
		try {
			if (!this.sendConnectRequest()) {
				// if the connection failed wee why and notify the user
				String failReason = this.socket.readLine();
				this.onConnectionFailed.accept(failReason);
				return;
			}
			this.onConnectionSuccess.run();
		} catch (IOException e) {
			// TODO: HANDLE EXCEPTION
			this.onError.accept(ShareBook.getMessagesBundle().getString("chat_init_error"));
			e.printStackTrace();
		}

		// block & process incoming data
		try {
			while (!this.socket.isClosed())
				this.handleServerResponses();
		} catch (IOException e) {
			e.printStackTrace();
			this.onError.accept(ShareBook.getMessagesBundle().getString("chat_send_msg_error"));
		}
	}

	/**
	 * Handles server responses, specifically {@link ServerResponseType#MESSAGE} and {@link ServerResponseType#USER_CONNECTED}
	 *
	 * @throws IOException when error while reading from the socket
	 */
	private void handleServerResponses() throws IOException {
		String header = this.socket.readLine();

		ServerResponseType resType = ServerResponseType.fromHeader(header);

		if (resType == ServerResponseType.MESSAGE) {
			// read from ID
			int from_id = Integer.parseInt(this.socket.readLine());

			// skip to id because its your own chat_uid
			this.socket.readLine();

			this.socket.readLine(); // skip begin message header

			// read base64 message
			String message = new String(
				Base64.getDecoder().decode( // decode base64 message
					this.socket.readLine().getBytes(StandardCharsets.UTF_8)
				),
				StandardCharsets.UTF_8
			);

			this.socket.readLine(); // skipp end message header

			this.onMessage.accept(from_id, message);
		} else if (resType == ServerResponseType.USER_CONNECTED) {
			int new_user_chat_id = Integer.parseInt(this.socket.readLine());
			String username = this.socket.readLine();
			System.out.print("User connected");
			System.out.print(" id: " + new_user_chat_id);
			System.out.println(" username: " + username);

			this.onUserConnected.accept(new ChatUser(new_user_chat_id, username));
		}
	}

	/// SETTERS ///

	/**
	 * Set the on connection established callback
	 * <p>
	 * This should be set before calling {@link Thread#start()} on this object
	 * otherwise this will throw an exception
	 *
	 * @param onConnectionSuccess the callback to execute
	 * @throws IllegalStateException if the thread has started
	 */
	public void setOnConnectionSuccess(@NotNull Runnable onConnectionSuccess) {
		if (this.isAlive())
			throw new IllegalStateException("Thread is already running");

		this.onConnectionSuccess = Objects.requireNonNull(onConnectionSuccess);
	}

	/**
	 * Set the on connection established callback
	 * <p>
	 * This should be set before calling {@link Thread#start()} on this object
	 * otherwise this will throw an exception
	 *
	 * @param onMessage the callback to execute. The first param is the uid for the user sending the message
	 *                  the second param is the message
	 * @throws IllegalStateException if the thread has started
	 */
	public void setOnMessage(@NotNull BiConsumer<Integer, String> onMessage) {
		if (this.isAlive())
			throw new IllegalStateException("Thread is already running");

		this.onMessage = Objects.requireNonNull(onMessage);
	}

	/**
	 * Set the on connection failed callback
	 * <p>
	 * This should be set before calling {@link Thread#start()} on this object
	 * otherwise this will throw an exception
	 *
	 * @param onConnectionFailed the callback to execute. The parameter for the callback is a {@link String} telling
	 *                           the reason of the failed connection
	 * @throws IllegalStateException if the thread has started
	 */
	public void setOnConnectionFailed(@NotNull Consumer<String> onConnectionFailed) {
		if (this.isAlive())
			throw new IllegalStateException("Thread is already running");

		this.onConnectionFailed = Objects.requireNonNull(onConnectionFailed);
	}

	/**
	 * Set the on user connected callback
	 * <p>
	 * This should be set before calling {@link Thread#start()} on this object
	 * otherwise this will throw an exception
	 *
	 * @param onUserConnected the callback to execute. The parameter for the callback is a {@link ChatUser} with
	 *                        the info of the connected user
	 * @throws IllegalStateException if the thread has started
	 */
	public void setOnUserConnected(@NotNull Consumer<ChatUser> onUserConnected) {
		if (this.isAlive())
			throw new IllegalStateException("Thread is already running");

		this.onUserConnected = Objects.requireNonNull(onUserConnected);
	}

	public int getChatUID() {
		return chat_uid;
	}

	synchronized public static Thread getThread() {
		return ChatClient.instance;
	}

	public static class Builder {
		private char[] loginHash;

		// callbacks
		@NotNull
		private Consumer<String> onError = System.err::println;
		@NotNull
		private Consumer<String> onConnectionFailed = System.err::println;
		@NotNull
		private BiConsumer<Integer, String> onMessage = (uid, msg) -> System.out.println("Message from " + uid + " \"" + msg + '"');
		@NotNull
		private Consumer<ChatUser> onUserConnected = System.out::println;
		@NotNull
		private Runnable onConnectionSuccess = () -> {
		};

		public Builder loginHash(char[] loginHash) {
			this.loginHash = loginHash;
			return this;
		}

		public Builder onError(@NotNull Consumer<String> onError) {
			this.onError = onError;
			return this;
		}

		public Builder onMessage(BiConsumer<Integer, String> onMessage) {
			this.onMessage = onMessage;
			return this;
		}

		public Builder onUserConnected(Consumer<ChatUser> onUserConnected) {
			this.onUserConnected = onUserConnected;
			return this;
		}

		public Builder onConnectionSuccess(Runnable onConnectionSuccess) {
			this.onConnectionSuccess = onConnectionSuccess;
			return this;
		}

		public Builder setOnConnectionFailed(Consumer<String> onConnectionFailed) {
			this.onConnectionFailed = onConnectionFailed;
			return this;
		}

		public ChatClient createChatClient() throws InstanceAlreadyExistsException {
			return new ChatClient(
				loginHash,
				onMessage,
				onUserConnected,
				onError,
				onConnectionSuccess,
				onConnectionFailed
			);
		}
	}
}

package org.sharebook.chat;

import org.graalvm.compiler.phases.common.PropagateDeoptimizeProbabilityPhase;
import org.jetbrains.annotations.NotNull;
import org.sharebook.ShareBook;

import javax.management.InstanceAlreadyExistsException;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Properties;
import java.util.function.Consumer;

public class ChatClient extends Thread {
	private static volatile Thread thread;

	// the SSL socket
	@NotNull
	private SSLSocket socket;

	// the BufferedWriter containing the output stream from the socket
	@NotNull
	private BufferedWriter socketOutStream;

	private final char[] loginHash;
	private int chat_uid;

	// callbacks
	private final Consumer<String> onError;
	private final Consumer<String> onMessage;
	private final Consumer<String> onUserConnected;

	/**
	 * Creates a new chat client
	 * @param loginHash the hash required to start the own handshake with the chat server
	 *                  this is required so the server can tell who is talking to
	 * @throws InstanceAlreadyExistsException if this class was already instantiated
	 */
	public ChatClient(final char[] loginHash, Consumer<String> onMessage, Consumer<String> onUserConnected, Consumer<String> onError) throws InstanceAlreadyExistsException {
		this.loginHash = loginHash;
		this.onMessage = onMessage;
		this.onUserConnected = onUserConnected;
		this.onError = onError;

		this.setDaemon(true); // the client thread SHOULD be a daemon thread, NOT a user thread

		if (ChatClient.thread != null)
			throw new InstanceAlreadyExistsException(
				"The " + this.getClass().getName() + " class can only be instantiated once!"
			);

		ChatClient.thread = this;
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
		if (Thread.currentThread().isInterrupted())
			return;

		Properties appConfig = ShareBook.getAppConfig();

		SSLSocketFactory factory = this.configuredSSLContext("/resources/cert.pem").getSocketFactory();
		this.socket = (SSLSocket) factory.createSocket(
			appConfig.getProperty("CHAT_IP", "127.0.0.1"),
			Integer.parseInt(appConfig.getProperty("CHAT_PORT", "12365"))
		);
		this.socket.startHandshake();

		// get and set the out stream to be used within the class
		this.socketOutStream = new BufferedWriter( // improve performance with a buffer
			new OutputStreamWriter( // encode raw bytes to the default charset
				this.socket.getOutputStream(),
				StandardCharsets.UTF_8
			)
		);
	}

	/**
	 * Configures the SSL context to accept a self-signed certificate
	 * @return an SSLContext object that will trust the given certificate
	 * @throws CertificateException when the certificate X.509 is not supported
	 * @throws KeyStoreException if there is no KeyStore (this exception is unlikely to happen)
	 * @throws IOException if there was an error while reading the certificate
	 * @throws NoSuchAlgorithmException if the TLSv1.2 is not supported
	 * @throws KeyManagementException if the operation (forcing the created ssl context to trust the self-signed cert) fails
	 */
	private SSLContext configuredSSLContext(String certFilePath) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, KeyManagementException {
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
	 * @throws IOException if there was an error while writing to {@link #socketOutStream} or reading from the socket
	 */
	private void sendConnectRequest() throws IOException {
		if (Thread.currentThread().isInterrupted())
			return;

		// send connect request to the chat server, this will start our own handshake with the server
		this.socketOutStream.write("CONNECT:" + String.copyValueOf(loginHash));
		this.socketOutStream.flush(); // send data immediately

		BufferedReader inReader = new BufferedReader( // improve performance with a buffer
			new InputStreamReader( // decode raw bytes to the default charset
				this.socket.getInputStream(),
				StandardCharsets.UTF_8
			)
		);

		// the response for the initial handshake should be single line
		String connServerHandshake = inReader.readLine();
		System.out.println(connServerHandshake);
	}

	private void writeData(String data) {
		try {
			this.socketOutStream.write(data);
			this.socketOutStream.flush(); // flush the buffer and send data immediately
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
			return;
		}
	}

	/**
	 * Use this method to simply send a message tto someone
	 *
	 * This method is synchronized to avoid multiple threads sending a message at one time
	 * @param message the message you want to send
	 * @throws InterruptedException if the thread is interrupted
	 */
	synchronized public void sendMessage(String message) throws InterruptedException {
		if (Thread.currentThread().isInterrupted())
			throw new InterruptedException("The chat thread is interrupted");

		this.writeData("MESSAGE:" + this.chat_uid + ":" + message);
	}

	@Override
	public void run() {
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

		try {
			this.sendConnectRequest();
		} catch (IOException e) {
			// TODO: HANDLE EXCEPTION
			this.onError.accept(ShareBook.getMessagesBundle().getString("chat_init_error"));
			e.printStackTrace();
		}
	}

	synchronized public static Thread getThread() {
		return ChatClient.thread;
	}

	synchronized public static boolean alreadyInitialized() {
		return ChatClient.thread != null;
	}
}

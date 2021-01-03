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

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ChatSocket implements AutoCloseable {
	private final Socket clientSocket;
	private final BufferedWriter socketBuffOutStream;
	private final BufferedReader socketBuffInStream;

	public Runnable onClose;

	public ChatSocket(Socket clientSocket) throws IOException {
		this.clientSocket = clientSocket;

		this.socketBuffInStream = new BufferedReader( // improve performance with a buffer
			new InputStreamReader( // decode raw bytes to the default charset
				this.clientSocket.getInputStream(),
				StandardCharsets.UTF_8
			)
		);

		this.socketBuffOutStream = new BufferedWriter( // improve performance with a buffer
			new OutputStreamWriter( // encode raw bytes to the default charset
				this.clientSocket.getOutputStream(),
				StandardCharsets.UTF_8
			)
		);
	}

	/**
	 * Writes multiple lines to the socket, by appending a line separator after each written string
	 * <p>
	 * This method will call flush on the stream at the end, so data will be sent
	 *
	 * @param lines the lines you want to write in the socket stream. These lines may not contain a new line
	 *              as it will be added by this method automatically when writing to the buffer
	 * @return the same object. Just for convenience so you can chain calls to other methods
	 * @throws IOException if there was an error writing to the buffer
	 */
	public ChatSocket writeLines(String... lines) throws IOException {
		for (String line : lines) {
			this.socketBuffOutStream.write(line);
			this.socketBuffOutStream.newLine();
		}

		this.socketBuffOutStream.flush();

		return this;
	}

	/**
	 * @return The original {@link Socket} object. The same used as argument in the
	 * {@link #ChatSocket(Socket)} constructor
	 */
	public Socket getClientSocket() {
		return clientSocket;
	}

	/**
	 * This method behaves exacly as {@link BufferedReader#readLine()}
	 *
	 * @return the line read or null if EOF is reached, see {@link BufferedReader#readLine()} for more
	 * @throws IOException if there was an error reading the line
	 */
	public String readLine() throws IOException {
		return this.socketBuffInStream.readLine();
	}

	public BufferedWriter getSocketBuffOutStream() {
		return socketBuffOutStream;
	}

	public BufferedReader getSocketBuffInStream() {
		return socketBuffInStream;
	}

	/**
	 * @return true if the socket is closed, this could be because you closed it or because EOF has been reached
	 */
	public boolean isClosed() {
		boolean eof_reached = false;

		try {
			// read from the stream to check if we've reached the EOF
			// this operation should not modify the current position (pointer) in the buffer
			// therefore a mark and reset is used

			// mark position previous to reading from the stream, so we can go reset the pointer
			this.socketBuffInStream.mark(1);
			eof_reached = this.socketBuffInStream.read() == -1;
			this.socketBuffInStream.reset(); // reset the pointer
		} catch (IOException e) {
			if ("Stream closed".equalsIgnoreCase(e.getMessage()))
				return true;
			e.printStackTrace();
		}

		return this.clientSocket.isClosed() || eof_reached;
	}

	/**
	 * Adds a hook to execute automatically when {@link #close()} is called
	 *
	 * @param onClose the hook to execute
	 */
	public void setOnCloseHook(Runnable onClose) {
		this.onClose = onClose;
	}

	/**
	 * Will close the in/out streams opened from the socket
	 * <p>
	 * Note: the first thing this method will do is execute the on close hook if it was set via {@link #setOnCloseHook(Runnable)}
	 * this is done to ensure it is executed even in subsequent calls to close other resources fail
	 * <p>
	 * And then the socket itself will be closed
	 *
	 * @throws IOException if something bad happens while closing everything
	 */
	@Override
	public void close() throws IOException {
		if (this.onClose != null)
			this.onClose.run();

		this.socketBuffOutStream.close();
		this.socketBuffInStream.close();
		this.clientSocket.close();
	}
}

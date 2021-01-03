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
import org.junit.jupiter.api.Test;

import javax.management.InstanceAlreadyExistsException;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatClientTest {
	int N_TESTS = 10_000;
	int n_test = 0;
	String message = "Hola wey";

	public ChatClientTest() {
		ShareBook.loadConfig();
		ShareBook.changeMessagesBundle(Locale.ENGLISH);
	}

	@Test
	public void test() throws InstanceAlreadyExistsException, InterruptedException {
		ChatClient chatClient = new ChatClient.Builder()
			.loginHash("loginhashbypasss".toCharArray())
			.onUserConnected(System.out::println)
			.createChatClient();
		chatClient.setOnMessage((uid, message) -> {
			// receive the message from yourself
			System.out.print("\b\b\b\b\b" + this.n_test);
			assertEquals(this.message, message);
			if (this.n_test >= this.N_TESTS) {
				chatClient.interrupt();
				return;
			}
			try {
				chatClient.sendMessage(this.message, chatClient.getChatUID());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			++this.n_test;
		});
		chatClient.setOnConnectionSuccess(() -> {
			try {
				// send a message to yourself
				chatClient.sendMessage(this.message, chatClient.getChatUID());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
		chatClient.setDaemon(false);
		chatClient.start();
		chatClient.join();
	}
}
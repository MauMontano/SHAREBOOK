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

import org.jetbrains.annotations.NotNull;

/**
 * Wrapper class containing information about a connected user in the chat
 */
public class ChatUser {
	// the chat UID, this is used to identify to who/from a message is sent/received
	private final int chat_uid;

	// the username of the user associated with the chatUID
	@NotNull
	private final String userName;

	public ChatUser(int chat_uid, @NotNull String userName) {
		this.chat_uid = chat_uid;
		this.userName = userName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ChatUser chatUser = (ChatUser) o;
		return chat_uid == chatUser.chat_uid;
	}

	@Override
	public int hashCode() {
		return chat_uid;
	}

	@Override
	public String toString() {
		return "ChatUser{" +
			"chat_uid=" + chat_uid +
			", userName='" + userName + '\'' +
			'}';
	}
}

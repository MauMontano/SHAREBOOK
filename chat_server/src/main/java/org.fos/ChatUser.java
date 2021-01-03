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

import java.util.HashMap;
import org.jetbrains.annotations.NotNull;

public class ChatUser {
	private final int hash_code;

	@NotNull
	private final String username;

	public ChatUser(int hash_code, @NotNull String username) {
		this.hash_code = hash_code;
		this.username = username;
	}

	public int getHashCode() {
		return hash_code;
	}

	public @NotNull String getUsername() {
		return username;
	}

	@Override
	public int hashCode() {
		return this.hash_code;
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;

		if (o == null || getClass() != o.getClass()) return false;

		ChatUser chatUser = (ChatUser) o;

		return hash_code == chatUser.hash_code;
	}

	@Override
	public String toString() {
		return "ChatUser{" +
			"hash_code=" + hash_code +
			", username='" + username + '\'' +
			'}';
	}
}

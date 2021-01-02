package org.sharebook.chat;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Wrapper class containing information about a connected user in the chat
 */
public class ChatUser {
	// the chat UID, this is used to identify to who/from a message is sent/received
	private int chatUID;

	// the username of the user associated with the chatUID
	@NotNull
	private String userName;

	public ChatUser(@NotNull int chatUID, @NotNull String userName) {
		this.chatUID = chatUID;
		this.userName = userName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ChatUser chatUser = (ChatUser) o;
		return chatUID == chatUser.chatUID;
	}

	@Override
	public int hashCode() {
		return chatUID;
	}
}

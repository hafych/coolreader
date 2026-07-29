/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

/**
 * Owns one exact OPEN_DOCUMENT_TREE command and argument.
 */
public final class DocumentTreeRequestState<T> {
	public enum Command {
		DELETE_FILE(1),
		DELETE_FOLDER(2),
		SAVE_LOGCAT(3);

		private final int code;

		Command(int code) {
			this.code = code;
		}

		public int getCode() {
			return code;
		}

		public static Command fromCode(int code) {
			for (Command command : values()) {
				if (command.code == code)
					return command;
			}
			return null;
		}
	}

	private Request<T> current;

	public synchronized Request<T> begin(
			Command command,
			T argument) {
		if (current != null
				|| command == null
				|| argument == null)
			return null;
		current = new Request<>(command, argument);
		return current;
	}

	public synchronized Request<T> peek() {
		return current;
	}

	public synchronized Request<T> take() {
		Request<T> request = current;
		current = null;
		return request;
	}

	public synchronized boolean cancel(Request<T> request) {
		if (request == null || current != request)
			return false;
		current = null;
		return true;
	}

	public synchronized boolean isPending() {
		return current != null;
	}

	public static final class Request<T> {
		private final Command command;
		private final T argument;

		private Request(Command command, T argument) {
			this.command = command;
			this.argument = argument;
		}

		public Command getCommand() {
			return command;
		}

		public T getArgument() {
			return argument;
		}
	}
}

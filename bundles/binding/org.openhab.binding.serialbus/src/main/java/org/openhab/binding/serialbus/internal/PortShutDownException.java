package org.openhab.binding.serialbus.internal;

public class PortShutDownException extends Exception {
	private static final long serialVersionUID = 2766613156498994999L;

	public PortShutDownException(final String message) {
		super(message);
	}
}

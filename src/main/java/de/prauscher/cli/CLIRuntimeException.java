package de.prauscher.cli;

public class CLIRuntimeException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public CLIRuntimeException(String message) {
		super(message);
	}
}

package de.prauscher.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class CLI {
	/**
	 * Print help of a given command and a list of arguments
	 * @param command Command for which to print help
	 */
	@Command(command = "help", help = "print detailed help of a given command")
	public void printHelp(@CommandArgument(help = "command for which help should be provided") String command) {
		boolean found = false;
		for (Method m : this.getClass().getMethods()) {
			Command cmd = m.getAnnotation(Command.class);
			if (cmd != null && cmd.command().equalsIgnoreCase(command)) {
				if (found) {
					System.out.println("----");
				}
				System.out.println("Command " + cmd.command() + " (" + m.getParameterTypes().length + " parameters): " + cmd.help());
				for (int i = 0; i < m.getParameterTypes().length; i++) {
					String argumentHelp = "no help given";
					for (Annotation a : m.getParameterAnnotations()[i]) {
						if (CommandArgument.class.isAssignableFrom(a.annotationType())) {
							CommandArgument argument = (CommandArgument)a;
							argumentHelp = argument.help();
						}
					}
					System.out.printf("@param %s: %s \n", m.getParameterTypes()[i].getSimpleName(), argumentHelp);
				}
				found = true;
			}
		}
		if (!found) {
			System.out.println("Command " + command + " not found");
		}
	}

	/**
	 * Print a list of all available commands
	 */
	@Command(command = "help", help = "list all available commands")
	public void printCommandList() {
		System.out.println("List of possible commands:");
		for (Method m : this.getClass().getMethods()) {
			Command cmd = m.getAnnotation(Command.class);
			if (cmd != null) {
				System.out.printf("%15s %d - %s\n", cmd.command(), m.getParameterTypes().length, cmd.help());
			}
		}
		System.out.printf("%15s %d - %s\n", "quit", 0, "quits the cli");
		System.out.println("Choose wisely. Use help <command> if unsure!");
	}

	/**
	 * (intelligent) cast of argument, considering primitives and enums
	 * @param argument Input given on command line
	 * @param type Target type to return
	 * @return argument as provided class
	 */
	@SuppressWarnings("unchecked")
	private static Object convertArgument(String argument, Class<?> type) {
		if (boolean.class.isAssignableFrom(type)) {
			return Boolean.parseBoolean(argument);
		}
		if (byte.class.isAssignableFrom(type)) {
			return Byte.parseByte(argument);
		}
		if (short.class.isAssignableFrom(type)) {
			return Short.parseShort(argument);
		}
		if (int.class.isAssignableFrom(type)) {
			return Integer.parseInt(argument);
		}
		if (long.class.isAssignableFrom(type)) {
			return Long.parseLong(argument);
		}
		if (char.class.isAssignableFrom(type)) {
			return argument.charAt(0);
		}
		if (float.class.isAssignableFrom(type)) {
			return Float.parseFloat(argument);
		}
		if (double.class.isAssignableFrom(type)) {
			return Double.parseDouble(argument);
		}
		if (type.isEnum()) {
			return Enum.valueOf(type.asSubclass(Enum.class), argument);
		}
		return type.cast(argument);
	}

	/**
	 * Convert multiple arguments
	 * @param args List of arguments as String
	 * @param types Array of classes needed
	 * @return Array of converted arguments
	 */
	private static Object[] convertArguments(List<String> args, Class<?>[] types) {
		Object[] params = new Object[args.size()];
		for (int i = 0; i < args.size(); i++) {
			params[i] = convertArgument(args.get(i), types[i]);
		}
		return params;
	}

	/**
	 * Find correct command, convert Arguments as needed and execute handler
	 * @param command Command given on command line
	 * @param args List of arguments as String
	 */
	private void executeCommand(String command, List<String> args) {
		for (Method m : this.getClass().getMethods()) {
			Command cmd = m.getAnnotation(Command.class);
			if (cmd != null && cmd.command().equalsIgnoreCase(command) && args.size() == m.getParameterTypes().length) {
				try {
					m.invoke(this, convertArguments(args, m.getParameterTypes()));
					return;
				} catch (NumberFormatException e) {
					System.out.println("invalid numeric parameter given");
					return;
				} catch (InvocationTargetException e) {
					Throwable cause = e.getCause();

					if (cause instanceof CLIRuntimeException) {
						System.out.println("failed to execute command: " + cause.getMessage());
					}
					else {
						cause.printStackTrace();
					}

					return;
				} catch (IllegalAccessException e) {
					e.printStackTrace();
					return;
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					return;
				}
			}
		}
		System.out.println("Unknown command or wrong parameter count");
	}

	/**
	 * Parse a single inputline
	 * @param line Line entered after prompt
	 * @return false iff command was "quit"
	 */
	private boolean handleInput(String line) {
		ArrayList<String> args = tokenizeString(line);
		if (args.size() == 0)  {
			return true;
		}

		String command = args.remove(0);

		if (command.equalsIgnoreCase("quit")) {
			return false;
		} else {
			executeCommand(command, args);
		}
		return true;
	}

	/**
	 * split a string, considering arguments in quotes as single arguments
	 * @param line String to split
	 * @return List of single Strings
	 */
	private static ArrayList<String> tokenizeString(String line) {
		int i = 0;
		ArrayList<String> args = new ArrayList<String>();
		while (i < line.length()) {
			int end;
			if (line.charAt(i) == '"') {
				// Go over leading \"
				i++;
				end = line.indexOf('"', i);
			} else {
				end = line.indexOf(" ", i);
			}
			if (end < 0) {
				end = line.length();
			}
			// Filter out empty strings
			if (i != end) {
				args.add(line.substring(i, end));
			}
			i = end + 1;
		}
		return args;
	}

	/**
	 * Start CLI-loop including printing the prompt, reading the input and passing parsed arguments to handlers until quit is given. Using default prompt "&gt; "
	 * @throws IOException If an I/O error occurs in BufferedReader.readLine
	 */
	public void loop() throws IOException {
		loop("> ");
	}

	/**
	 * Start CLI-loop including printing the prompt, reading the input and passing parsed arguments to handlers until quit is given.
	 * @param prompt the prompt to use (e.g. "&gt; ")
	 * @throws IOException If an I/O error occurs in BufferedReader.readLine
	 */
	public void loop(String prompt) throws IOException {
		String line;
		do {
			line = readLine(prompt);
		} while (handleInput(line));
	}

	/**
	 * Prompt for a single line-input
	 * @param prompt the prompt to use (e.g. "&gt; ")
	 * @return the raw line given by the user
	 * @throws IOException If an I/O error occurs in BufferedReader.readLine
	 */
	public String readLine(String prompt) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		System.out.print(prompt);
		return reader.readLine();
	}
}

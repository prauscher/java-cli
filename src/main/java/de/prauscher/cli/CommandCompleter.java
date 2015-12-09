package de.prauscher.cli;

import java.lang.reflect.Method;

import java.util.LinkedList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;


public class CommandCompleter implements Completer {
	private final List<Completer> completers;

	public CommandCompleter(CLI cli) {
		this.completers = generateCompleters(cli);
	}

	@Override
	public int complete(String buffer, int cursor, java.util.List<CharSequence> candidates) {
		Set<CharSequence> candidatesSet = new HashSet<CharSequence>();

		int longestMatch = -1;

		for (Completer c : completers) {
			java.util.List<CharSequence> tmpCandidates = new java.util.LinkedList<CharSequence>();

			int cMatch = c.complete(buffer, cursor, tmpCandidates);

			candidatesSet.addAll(tmpCandidates);
			longestMatch = Math.max(longestMatch, cMatch);
		}

		candidates.addAll(candidatesSet);
		return longestMatch;
	}

	private static List<Completer> generateCompleters(CLI cli) {
		List<Completer> completers = new LinkedList<Completer>();

		for (Method m : cli.getClass().getMethods()) {
			Command cmd = m.getAnnotation(Command.class);
			if (cmd != null) {
				completers.add(new StringsCompleter(cmd.command()));
			}
		}

		completers.add(new StringsCompleter("quit"));

		return completers;
	}
}

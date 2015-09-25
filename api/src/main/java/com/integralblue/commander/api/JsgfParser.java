package com.integralblue.commander.api;

import java.util.Collection;
import java.util.Set;

public interface JsgfParser extends Plugin {

	/**
	 * Returns all possible matching sentences for a given collection of rules
	 *
	 * @param rules
	 * @return
	 */
	Set<String> getAllPossibleSentences(Collection<JsgfRule> rules);

	/**
	 * Given a collection of rules, matches the text against the rules, and
	 * returned the result.
	 *
	 * Matching of the text against the rules is case insensitive.
	 *
	 * Note that method may be called by multiple threads concurrently, so it
	 * must be thread safe!
	 *
	 * @param rules
	 * @param text
	 * @return null if the text does not match any rule
	 */
	MatchedRule getMatchedRule(Collection<JsgfRule> rules, String text);
}

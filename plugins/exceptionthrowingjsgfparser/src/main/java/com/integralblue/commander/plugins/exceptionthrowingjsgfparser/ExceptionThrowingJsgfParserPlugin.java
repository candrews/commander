package com.integralblue.commander.plugins.exceptionthrowingjsgfparser;

import java.util.Collection;
import java.util.Set;

import com.integralblue.commander.api.AbstractPlugin;
import com.integralblue.commander.api.JsgfParser;
import com.integralblue.commander.api.JsgfRule;
import com.integralblue.commander.api.MatchedRule;

/**
 * JsgfParser implementation that will throw an exception if an attempt is made
 * to use it. This plugin is useful when using your configuration would never
 * use a JsgfParser (such as when using Grammar Engine plugin that does its own
 * Jsgf parsing).
 */
public class ExceptionThrowingJsgfParserPlugin extends AbstractPlugin implements JsgfParser {

	@Override
	public Set<String> getAllPossibleSentences(Collection<JsgfRule> rules) {
		throw new RuntimeException(ExceptionThrowingJsgfParserPlugin.class.getName()
				+ " is intended to only be used if no other plugins require a JsgfParser - in other words, only use this plugin if all other plugins (such as the Grammar Engine) do not require a JsgfParser. An attempt was made to use this plugin to parse Jsgf - so the configuration is incorrect.");

	}

	@Override
	public MatchedRule getMatchedRule(Collection<JsgfRule> rules, String text) {
		throw new RuntimeException(ExceptionThrowingJsgfParserPlugin.class.getName()
				+ " is intended to only be used if no other plugins require a JsgfParser - in other words, only use this plugin if all other plugins (such as the Grammar Engine) do not require a JsgfParser. An attempt was made to use this plugin to parse Jsgf - so the configuration is incorrect.");
	}

}

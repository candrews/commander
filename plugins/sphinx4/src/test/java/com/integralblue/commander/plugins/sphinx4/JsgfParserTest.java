package com.integralblue.commander.plugins.sphinx4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.integralblue.commander.api.JsgfRule;
import com.integralblue.commander.api.MatchedRule;

public class JsgfParserTest {

	@Test
	public void testGetAllPossibleSentences() throws Exception {
		try (Sphinx4JsgfParserPlugin parser = new Sphinx4JsgfParserPlugin()) {
			final Set<String> expected = new HashSet<>(Arrays.asList(new String[] { "Hello World", "Good Bye" }));

			Set<String> actual = parser.getAllPossibleSentences(Collections.singleton(JsgfRule.builder().name("command")
					.jsgf("(Hello World) {Hello World Tag} | (Good Bye) {Good Bye Tag}").build()));
			assertEquals(expected, actual);
		}
	}

	@Test
	public void testMatchedRule() throws Exception {
		try (Sphinx4JsgfParserPlugin parser = new Sphinx4JsgfParserPlugin()) {
			MatchedRule matchedRule = parser.getMatchedRule(
					Collections.singleton(JsgfRule.builder().name("command")
							.jsgf("(hello world) {hello world tag} | (good bye) {good bye tag}").build()),
					"hello world");
			assertNotNull(matchedRule);
			assertEquals(Collections.singletonList("hello world tag"), matchedRule.getTags());
			assertEquals("command", matchedRule.getName());
		}
	}

	@Test
	public void testMatchedRuleMixedCase() throws Exception {
		try (Sphinx4JsgfParserPlugin parser = new Sphinx4JsgfParserPlugin()) {
			MatchedRule matchedRule = parser.getMatchedRule(
					Collections.singleton(JsgfRule.builder().name("command")
							.jsgf("(Hello World) {Hello World Tag} | (Good Bye) {Good Bye Tag}").build()),
					"hElLo WoRlD");
			assertNotNull(matchedRule);
			assertEquals(Collections.singletonList("Hello World Tag"), matchedRule.getTags());
			assertEquals("command", matchedRule.getName());
		}
	}

}

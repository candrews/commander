package com.integralblue.commander.plugins.sphinx4;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.integralblue.commander.api.AbstractPlugin;
import com.integralblue.commander.api.JsgfParser;
import com.integralblue.commander.api.JsgfRule;
import com.integralblue.commander.api.MatchedRule;

import edu.cmu.sphinx.jsgf.JSGFGrammar;
import edu.cmu.sphinx.jsgf.parser.JSGFParser;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.linguist.language.grammar.GrammarArc;
import edu.cmu.sphinx.linguist.language.grammar.GrammarNode;

public class Sphinx4JsgfParserPlugin extends AbstractPlugin implements JsgfParser {
	private final static URL baseURL;

	/**
	 * Since the same rules may be queried multiple times, cache the JSGFGrammar
	 * for given rules
	 */
	private static final Map<Collection<JsgfRule>, JSGFGrammar> rulesToGrammarsCache = Collections
			.synchronizedMap(new WeakHashMap<Collection<JsgfRule>, JSGFGrammar>());

	static {
		final String url = Sphinx4JsgfParserPlugin.class.getResource("/grammar/empty.gram").toString();
		try {
			baseURL = new URL(url.substring(0, url.length() - "empty.gram".length()));
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	private static MatchedRule getMatchedRule(JSGFGrammar jsgfGrammar, String text) {
		RuleParse ruleParse = RuleParser.parse(text, jsgfGrammar, null);
		if (ruleParse == null) {
			return null;
		} else {
			return MatchedRule.builder().name(ruleParse.getRuleReference().getRuleName()).tags(ruleParse.getTags())
					.build();
		}
	}

	private void addAllPossibleSentences(GrammarNode node, String sentenceSoFar, Collection<String> ret) {
		if (node.isFinalNode()) {
			ret.add(sentenceSoFar.trim());
		} else {
			if (!node.isEmpty()) {
				Word word = node.getWord();
				if (!word.isFiller()) {
					sentenceSoFar = sentenceSoFar + word.getSpelling() + ' ';
				}
			}
			for (GrammarArc arc : node.getSuccessors()) {
				addAllPossibleSentences(arc.getGrammarNode(), sentenceSoFar, ret);
			}
		}
	}

	@Override
	public Set<String> getAllPossibleSentences(Collection<JsgfRule> rules) {
		final Set<String> ret = new HashSet<>();
		final JSGFGrammar jsgfGrammar = getJSGFGrammarForRules(rules);
		try {
			GrammarNode node = jsgfGrammar.getInitialNode();
			addAllPossibleSentences(node, "", ret);
			return ret;
		} finally {
			jsgfGrammar.deallocate();
		}
	}

	private JSGFGrammar getJSGFGrammarForRules(Collection<JsgfRule> rules) {
		return rulesToGrammarsCache.computeIfAbsent(rules, ((unused) -> {
			try {
				JSGFGrammar jsgfGrammar = new JSGFGrammar(baseURL, "empty", false, false, false, false,
						DummyDictionary.INSTANCE);
				jsgfGrammar.allocate();
				jsgfGrammar.commitChanges();
				boolean foundPublicRule = false;
				for (JsgfRule rule : rules) {
					jsgfGrammar.getRuleGrammar().setRule(rule.getName(), JSGFParser.ruleForJSGF(rule.getJsgf()),
							rule.isPublic());
					if (rule.isPublic()) {
						foundPublicRule = true;
					}
				}
				if (!foundPublicRule) {
					throw new IllegalArgumentException("No public rules provided, so there is nothing to listen for.");
				}
				jsgfGrammar.commitChanges();
				return jsgfGrammar;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}));
	}

	@Override
	public MatchedRule getMatchedRule(Collection<JsgfRule> rules, String text) {
		JSGFGrammar jsgfGrammar = getJSGFGrammarForRules(rules);
		try {
			return getMatchedRule(jsgfGrammar, text);
		} finally {
			jsgfGrammar.deallocate();
		}
	}

}

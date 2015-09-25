package com.integralblue.commander.plugins.sphinx4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import com.integralblue.commander.api.AudioData;
import com.integralblue.commander.api.GrammarEngine;
import com.integralblue.commander.api.GrammarResult;
import com.integralblue.commander.api.JsgfParser;
import com.integralblue.commander.api.JsgfRule;
import com.integralblue.commander.api.MatchedRule;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.jsgf.JSGFGrammar;
import edu.cmu.sphinx.jsgf.JSGFGrammarException;
import edu.cmu.sphinx.jsgf.JSGFGrammarParseException;
import edu.cmu.sphinx.jsgf.parser.JSGFParser;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Sphinx4GrammarPlugin extends AbstractSphinx4RecognitionPlugin implements GrammarEngine {

	private final static String grammarPath;

	static {
		final String url = Sphinx4JsgfParserPlugin.class.getResource("/grammar/empty.gram").toString();
		grammarPath = url.substring(0, url.length() - "empty.gram".length());
	}

	private static void logRandomSentences(JSGFGrammar jsgfGrammar, int count) {
		Set<String> set = new HashSet<String>();
		for (int i = 0; i < count; i++) {
			String s = jsgfGrammar.getRandomSentence();
			if (!set.contains(s)) {
				set.add(s);
			}
		}
		List<String> sampleList = new ArrayList<String>(set);
		Collections.sort(sampleList);

		for (String sentence : sampleList) {
			log.debug(sentence);
		}
	}

	private JsgfParser jsgfParser;

	@Override
	protected Configuration getSphinxConfiguration() {
		Configuration configuration = super.getSphinxConfiguration();

		configuration.setGrammarPath(grammarPath);
		configuration.setGrammarName("empty");
		configuration.setUseGrammar(true);

		return configuration;
	}

	@Override
	public void initialize() throws Exception {
		super.initialize();
		jsgfParser = manager.getPlugin(config.getString("jsgfParser"), JsgfParser.class);
	}

	@Override
	public CompletionStage<GrammarResult> listenForGrammarAsync(final Collection<JsgfRule> rules) {
		return manager.getMicrophone().getVoiceAsync(audioFormat).thenCompose((audioInputStream) -> {
			return listenForGrammarAsync(rules, audioInputStream);
		});
	}

	@Override
	public CompletionStage<GrammarResult> listenForGrammarAsync(@NonNull Collection<JsgfRule> rules,
			final AudioData audioData) {
		final JSGFGrammar jsgfGrammar = context.getInstance(JSGFGrammar.class);
		boolean foundPublicRule = false;
		try {
			jsgfGrammar.loadJSGF("empty");
			jsgfGrammar.commitChanges();
			for (JsgfRule rule : rules) {
				// sphinx's dictionary is all lower case, so to get matches, all
				// expected text (the rules) has to be lower case
				// later, when the result is matched against the rule, the
				// original rules (in the original case) are used, and
				// case-insensitive rule matching is done so the tag names are
				// in the original case
				jsgfGrammar.getRuleGrammar().setRule(rule.getName(),
						JSGFParser.ruleForJSGF(rule.getJsgf().toLowerCase()), rule.isPublic());
				if (rule.isPublic()) {
					foundPublicRule = true;
				}
			}
			jsgfGrammar.commitChanges();
		} catch (IOException | JSGFGrammarParseException | JSGFGrammarException e) {
			throw new RuntimeException(e);
		}
		if (!foundPublicRule) {
			throw new IllegalArgumentException("No public rules provided, so there is nothing to listen for.");
		}
		log.debug("Listening for grammar");
		logRandomSentences(jsgfGrammar, 5);
		return listenForText(audioData).thenApply((sphinxResult) -> {
			List<String> tags;
			if (sphinxResult.getText().equals(GrammarResult.UNKNOWN_TEXT)) {
				tags = Collections.emptyList();
			} else {
				MatchedRule matchedRule = jsgfParser.getMatchedRule(rules, sphinxResult.getText());
				if (matchedRule == null) {
					throw new IllegalStateException(
							"The text matches because the recognizer says so, but MatchedRule is null - this should never happen. text: "
									+ sphinxResult.getText());
				}
				tags = matchedRule.getTags();
			}
			return GrammarResult.builder().text(sphinxResult.getText()).audioData(sphinxResult.getAudioData())
					.tags(Collections.unmodifiableList(tags)).build();
		});
	}

}

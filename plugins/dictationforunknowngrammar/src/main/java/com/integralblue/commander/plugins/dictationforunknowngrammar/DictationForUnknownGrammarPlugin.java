package com.integralblue.commander.plugins.dictationforunknowngrammar;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.apache.commons.codec.StringEncoder;
import org.apache.commons.codec.language.DoubleMetaphone;

import com.integralblue.commander.api.AbstractPlugin;
import com.integralblue.commander.api.AudioData;
import com.integralblue.commander.api.DictationEngine;
import com.integralblue.commander.api.DictationResult;
import com.integralblue.commander.api.GrammarEngine;
import com.integralblue.commander.api.GrammarResult;
import com.integralblue.commander.api.JsgfParser;
import com.integralblue.commander.api.JsgfRule;
import com.integralblue.commander.api.MatchedRule;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * A {@link GrammarEngine} that will delegate to another {@link GrammarEngine},
 * then, if the result is unknown, run a {@link DictationEngine} on the result
 * audio, then inspect the resulting dictation text to see if it matches,
 * closely enough, one of the grammar rules and if so returns a
 * {@link GrammarResult} expressing the match.
 *
 * This plugin's goal is to improve grammar recognition accuracy and usefulness.
 *
 */
@Slf4j
public class DictationForUnknownGrammarPlugin extends AbstractPlugin implements GrammarEngine {

	private DictationEngine dictationEngine;
	private GrammarEngine grammarEngine;
	private JsgfParser jsgfParser;

	/**
	 * Matches a {@link DictationResult} against the provided {@link JsgfRule}
	 * collection returning the resulting {@link GrammarResult}
	 *
	 * @param rules
	 * @param dictationResult
	 * @return
	 */
	@SneakyThrows
	protected GrammarResult dictationResultToGrammarResult(final Collection<JsgfRule> rules,
			final DictationResult dictationResult) {
		final GrammarResult.GrammarResultBuilder grammarResultBuilder = GrammarResult.builder()
				.dictationResult(dictationResult);
		if (dictationResult.isUnknown()) {
			log.debug("After dictation, still unknown");
			return grammarResultBuilder.text(GrammarResult.UNKNOWN_TEXT).tags(Collections.emptyList()).build();
		} else {
			final String text = dictationResult.getText();
			final MatchedRule matchedRule = jsgfParser.getMatchedRule(rules, text);
			if (matchedRule == null) {
				final StringEncoder encoder = new DoubleMetaphone();
				final String encodedText = encoder.encode(text);
				for (String sentence : jsgfParser.getAllPossibleSentences(rules)) {
					if (encodedText.equals(encoder.encode(sentence))) {
						log.debug(
								"No exact match against the grammar for the dictation result, but did get a match when doing fuzzy searching");
						return grammarResultBuilder.tags(jsgfParser.getMatchedRule(rules, sentence).getTags())
								.text(sentence).build();
					}
				}
				log.debug("No exact match or fuzzy match against the grammar");
				return grammarResultBuilder.text(GrammarResult.UNKNOWN_TEXT).tags(Collections.emptyList()).build();
			} else {
				log.debug("Got an exact match of the dictation result against the grammar");
				return grammarResultBuilder.tags(matchedRule.getTags()).text(text).build();
			}
		}
	}

	private CompletionStage<GrammarResult> handleGrammarResult(final Collection<JsgfRule> rules,
			GrammarResult grammarResult) {
		if (grammarResult.isUnknown()) {
			if (grammarResult.getDictationResult() != null) {
				return CompletableFuture
						.completedFuture(dictationResultToGrammarResult(rules, grammarResult.getDictationResult()));
			}
			if (grammarResult.getAudioData() != null) {
				return dictationEngine.listenForDictationAsync(grammarResult.getAudioData())
						.thenApply((dictationResult) -> {
							return dictationResultToGrammarResult(rules, dictationResult);
						});
			}
			return CompletableFuture.completedFuture(grammarResult);
		} else {
			return CompletableFuture.completedFuture(grammarResult);
		}
	}

	@Override
	public void initialize() throws Exception {
		super.initialize();
		dictationEngine = manager.getPlugin(config.getString("dictationEngine"), DictationEngine.class);
		grammarEngine = manager.getPlugin(config.getString("grammarEngine"), GrammarEngine.class);
		jsgfParser = manager.getPlugin(config.getString("jsgfParser"), JsgfParser.class);
	}

	@Override
	public CompletionStage<GrammarResult> listenForGrammarAsync(final Collection<JsgfRule> rules) {
		return grammarEngine.listenForGrammarAsync(rules).thenCompose((grammarResult) -> {
			return handleGrammarResult(rules, grammarResult);
		});
	}

	@Override
	public CompletionStage<GrammarResult> listenForGrammarAsync(final Collection<JsgfRule> rules,
			final AudioData audioData) {
		return grammarEngine.listenForGrammarAsync(rules, audioData).thenCompose((grammarResult) -> {
			return handleGrammarResult(rules, grammarResult);
		});
	}
}

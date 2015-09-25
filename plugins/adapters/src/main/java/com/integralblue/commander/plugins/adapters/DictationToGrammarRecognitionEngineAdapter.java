package com.integralblue.commander.plugins.adapters;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletionStage;

import com.integralblue.commander.api.AbstractPlugin;
import com.integralblue.commander.api.AudioData;
import com.integralblue.commander.api.DictationEngine;
import com.integralblue.commander.api.DictationResult;
import com.integralblue.commander.api.GrammarEngine;
import com.integralblue.commander.api.GrammarResult;
import com.integralblue.commander.api.JsgfParser;
import com.integralblue.commander.api.JsgfRule;
import com.integralblue.commander.api.MatchedRule;

public class DictationToGrammarRecognitionEngineAdapter extends AbstractPlugin implements GrammarEngine {

	private DictationEngine dictationEngine;
	private JsgfParser jsgfParser;

	private GrammarResult handleDictationResult(final Collection<JsgfRule> rules, DictationResult dictationResult) {
		final GrammarResult.GrammarResultBuilder grammarResultBuilder = GrammarResult.builder()
				.dictationResult(dictationResult);
		if (dictationResult.isUnknown()) {
			return grammarResultBuilder.text(GrammarResult.UNKNOWN_TEXT).tags(Collections.emptyList()).build();
		} else {
			final String text = dictationResult.getText();
			final MatchedRule matchedRule = jsgfParser.getMatchedRule(rules, text);
			if (matchedRule == null) {
				return grammarResultBuilder.text(GrammarResult.UNKNOWN_TEXT).tags(Collections.emptyList()).build();
			} else {
				return grammarResultBuilder.tags(matchedRule.getTags()).text(text).build();
			}
		}
	}

	@Override
	public void initialize() throws Exception {
		super.initialize();
		dictationEngine = manager.getPlugin(config.getString("dictationEngine"), DictationEngine.class);
		jsgfParser = manager.getPlugin(config.getString("jsgfParser"), JsgfParser.class);
	}

	@Override
	public CompletionStage<GrammarResult> listenForGrammarAsync(final Collection<JsgfRule> rules) {
		return dictationEngine.listenForDictationAsync().thenApply((dictationResult) -> {
			return handleDictationResult(rules, dictationResult);
		});
	}

	@Override
	public CompletionStage<GrammarResult> listenForGrammarAsync(Collection<JsgfRule> rules, AudioData audioData) {
		return dictationEngine.listenForDictationAsync(audioData).thenApply((dictationResult) -> {
			return handleDictationResult(rules, dictationResult);
		});
	}
}

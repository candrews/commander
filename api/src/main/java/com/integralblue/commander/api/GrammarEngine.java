package com.integralblue.commander.api;

import java.util.Collection;
import java.util.concurrent.CompletionStage;

public interface GrammarEngine extends RecognitionEngine, Plugin {
	/**
	 * Listen for grammar.
	 *
	 * This method must be thread safe.
	 *
	 * @param rules
	 * @return
	 */
	CompletionStage<GrammarResult> listenForGrammarAsync(Collection<JsgfRule> rules);

	/**
	 * Listen for grammar.
	 *
	 * This method must be thread safe.
	 *
	 * @param rules
	 * @param audioData
	 *            source of audio for dictation
	 * @return
	 */
	CompletionStage<GrammarResult> listenForGrammarAsync(Collection<JsgfRule> rules, AudioData audioData);
}

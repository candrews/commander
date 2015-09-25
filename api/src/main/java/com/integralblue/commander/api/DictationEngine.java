package com.integralblue.commander.api;

import java.util.concurrent.CompletionStage;

public interface DictationEngine extends RecognitionEngine, Plugin {
	/**
	 * Listen for dictation.
	 *
	 * This method must be thread safe.
	 *
	 * @return
	 */
	CompletionStage<DictationResult> listenForDictationAsync();

	/**
	 * Listen for dictation.
	 *
	 * This method must be thread safe.
	 *
	 * @param audioData
	 *            source of audio for dictation
	 * @return
	 */
	CompletionStage<DictationResult> listenForDictationAsync(AudioData audioData);
}

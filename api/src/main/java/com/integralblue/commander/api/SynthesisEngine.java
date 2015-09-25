package com.integralblue.commander.api;

import java.util.concurrent.CompletionStage;

import com.integralblue.commander.Manager;

public interface SynthesisEngine extends Plugin {
	/**
	 * Speak the given text Note that method may be called by multiple threads
	 * concurrently, so it must be thread safe! Using
	 * {@link Manager#getSpeaker()} to get a {@link Speaker} to use to speak the
	 * text.
	 *
	 * @param text
	 * @return A {@link CompletionStage} to use to determine when the text is
	 *         done being spoken
	 */
	CompletionStage<Void> sayAsync(String text);
}

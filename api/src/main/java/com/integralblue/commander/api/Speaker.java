package com.integralblue.commander.api;

import java.util.concurrent.CompletionStage;

import javax.sound.sampled.AudioInputStream;

public interface Speaker extends Plugin {
	/**
	 * Play the given audioData to the speaker.
	 *
	 * @param audioData
	 */
	CompletionStage<Void> playAsync(AudioData audioData);

	/**
	 * Play the given audioInputStream to the speaker.
	 *
	 * @param audioInputStream
	 */
	CompletionStage<Void> playAsync(AudioInputStream audioInputStream);
}

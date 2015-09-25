package com.integralblue.commander.api;

import java.util.concurrent.CompletionStage;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public interface Microphone extends Plugin {

	/**
	 * Get the stream of audio data coming from the microphone. Note that no
	 * data is being written to this stream until {@link #startRecording()} is
	 * called. This method gets the stream in whatever format the microphone was
	 * setup to use - use the {@link #getStream(AudioFormat)} method to get the
	 * stream in a specific format.
	 *
	 * @return
	 */
	AudioInputStream getStream();

	/**
	 * Get the stream of audio data coming from the microphone. Note that no
	 * data is being written to this stream until {@link #startRecording()} is
	 * called. This method will transcode the stream from whatever format that
	 * microphone is using to the format specified by the audioFormat parameter.
	 *
	 * @return
	 */
	AudioInputStream getStream(AudioFormat audioFormat);

	/**
	 * Get voice data. The returned {@link AudioData} will have
	 * {@link AudioInputStream#getFrameLength()} set to a value other than
	 * {@link AudioSystem#NOT_SPECIFIED}. The stream will also start and end
	 * with minimal silence.
	 *
	 * @param audioFormat
	 * @return
	 */
	CompletionStage<AudioData> getVoiceAsync(AudioFormat audioFormat);

	/**
	 * Start listening to the microphone. Upon calling this method, audio data
	 * is written to the stream.
	 */
	void startRecording();

	/**
	 * Stops listening to the microphone. Upon calling this method, audio data
	 * will not be written to the stream.
	 */
	void stopRecording();
}

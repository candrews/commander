package com.integralblue.commander.plugins.localaudio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;

import com.integralblue.commander.api.AbstractPlugin;
import com.integralblue.commander.api.AudioData;
import com.integralblue.commander.api.Microphone;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocalMicrophonePlugin extends AbstractPlugin implements Microphone {
	/**
	 * Some data line cannot be re-opened once closed. Closing an
	 * AudioInputStream that was created from a TargetDataLine closes the
	 * TargetDataLine, and then the TargetDataLine may not be able to be
	 * re-opened, so don't propagate closing the AudioInputStream to the
	 * TargetDataLine.
	 */
	private static class NotClosingAudioInputStream extends AudioInputStream {

		public NotClosingAudioInputStream(TargetDataLine line) {
			super(line);
		}

		@Override
		public void close() throws IOException {
			// do nothing
		}

	}

	/**
	 * Gets the volume level for a given audioInputStream
	 *
	 * @param audioInputStream
	 * @return
	 */
	@SneakyThrows
	private static float getLevel(AudioInputStream audioInputStream) {
		if (audioInputStream.getFrameLength() == AudioSystem.NOT_SPECIFIED) {
			throw new IllegalArgumentException(
					"Cannot determine the level of an AudioInputStream of indeterminate length");
		}
		final AudioFormat ANALYSIS_FORMAT = new AudioFormat(audioInputStream.getFormat().getSampleRate(), 8, 1, true,
				false);
		// transcode to a format easier for analysis
		final AudioInputStream analysisAudioInputStream = audioInputStream.getFormat().equals(ANALYSIS_FORMAT)
				? audioInputStream : AudioSystem.getAudioInputStream(ANALYSIS_FORMAT, audioInputStream);
		byte[] buffer = new byte[(int) analysisAudioInputStream.getFrameLength()
				* analysisAudioInputStream.getFormat().getFrameSize()];
		int bytesRead = analysisAudioInputStream.read(buffer);
		assert(bytesRead == buffer.length);

		// calculate the RMS (root mean square)
		long sum = 0;
		for (byte aByte : buffer) {
			sum += aByte * aByte;
		}
		return (float) Math.sqrt(sum / bytesRead);
	}

	private float silenceToSoundThreshold;

	private float soundToSilenceThreshold;

	private float bufferSizeInSeconds;

	private TargetDataLine targetDataLine;

	private AudioInputStream audioInputStream;

	@Override
	public void close() throws Exception {
		super.close();
		audioInputStream.close();
		targetDataLine.close();
	}

	@Override
	public AudioInputStream getStream() {
		return new NotClosingAudioInputStream(targetDataLine);
	}

	@Override
	public AudioInputStream getStream(AudioFormat audioFormat) {
		if (audioFormat.equals(audioInputStream.getFormat())) {
			return audioInputStream;
		} else {
			if (AudioSystem.isConversionSupported(audioFormat, audioInputStream.getFormat())) {
				return AudioSystem.getAudioInputStream(audioFormat, audioInputStream);
			} else {
				throw new IllegalArgumentException(
						"Cannot directly record or convert the microphone line to the requested format. Format: "
								+ audioFormat);
			}
		}
	}

	@SneakyThrows
	private AudioInputStream getVoice() {
		// this method assumes that startRecording() has already been called

		// this method currently just looks at the volume and looks for the
		// presence of sound, not really for voice. It would be great if some
		// more sophisticated Voice Audio Detection(VAD) algorithm could be
		// implemented here.
		try (final AudioInputStream audioInputStream = getStream()) {
			final AudioFormat audioFormat = audioInputStream.getFormat();
			final byte[] buffer = new byte[(int) (audioFormat.getFrameSize() * audioFormat.getFrameRate()
					* bufferSizeInSeconds)];
			assert(buffer.length % audioFormat.getFrameSize() == 0);
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				boolean soundDetected = false;
				while (true) {
					final int bytesRead = audioInputStream.read(buffer);
					if (bytesRead == -1) {
						if (soundDetected) {
							// no more microphone data, so return what we have
							return new AudioInputStream(new ByteArrayInputStream(baos.toByteArray()), audioFormat,
									baos.size() / audioFormat.getFrameSize());
						} else {
							throw new RuntimeException("No sound detected and the microphone is not able to record");
						}
					} else if (bytesRead == 0) {
						// log.debug("No data received from microphone");
					} else {
						final float level = getLevel(
								new AudioInputStream(new ByteArrayInputStream(buffer, 0, bytesRead), audioFormat,
										bytesRead / audioFormat.getFrameSize()));
						log.debug("Level: {}", level);
						if (soundDetected) {
							baos.write(buffer, 0, bytesRead);
							if (level < soundToSilenceThreshold) {
								log.debug("Transitioned to silence");
								// transitioned to silence - we're done here
								return new AudioInputStream(new ByteArrayInputStream(baos.toByteArray()), audioFormat,
										baos.size() / audioFormat.getFrameSize());
							}
						} else if (level > silenceToSoundThreshold) {
							log.debug("Sound detected");
							// transitioned to sound
							baos.write(buffer, 0, bytesRead);
							soundDetected = true;
						}
					}
				}
			} finally {
				stopRecording();
			}
		}
	}

	@Override
	public CompletionStage<AudioData> getVoiceAsync(AudioFormat returnAudioFormat) {
		// Java doesn't have a way to do non-blocking io on audio input streams.
		// Even if Java did have a way to do no-blocking audio io, it probably
		// wouldn't be a good idea - non-blocking io works best on low bandwidth
		// and/or infrequent data, and audio data is both high bandwidth and
		// frequent, so the overhead of non-blocking io would outweigh the
		// thread-saving benefits.

		startRecording(); // immediately start recording so there's no delay
							// between the intent to record (which is expressed
							// by calling the getVoiceAsync method) and data
							// being captured. This prevents an awkward delay
							// where the user thinks the system is listening,
							// but it's not.
		return CompletableFuture.supplyAsync(() -> getVoice())
				.thenApply((voice) -> AudioSystem.getAudioInputStream(returnAudioFormat, voice))
				.thenApply((audioInputStream) -> {
					try {
						return AudioData.fromAudioInputStream(audioInputStream);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
	}

	@Override
	public void initialize() throws Exception {
		super.initialize();
		soundToSilenceThreshold = (float) config.getDouble("soundToSilenceThreshold");
		silenceToSoundThreshold = (float) config.getDouble("silenceToSoundThreshold");
		bufferSizeInSeconds = (float) config.getDouble("bufferSizeInSeconds");
		AudioFormat audioFormat = new AudioFormat((float) config.getDouble("audioFormat.sampleRate"),
				config.getInt("audioFormat.sampleSizeInBits"), config.getInt("audioFormat.channels"),
				config.getBoolean("audioFormat.signed"), config.getBoolean("audioFormat.bigEndian"));
		targetDataLine = AudioSystem.getTargetDataLine(audioFormat);
		targetDataLine.open(audioFormat);
		audioInputStream = new AudioInputStream(targetDataLine);
	}

	@Override
	public void startRecording() {
		targetDataLine.flush();
		targetDataLine.start();
	}

	@Override
	public void stopRecording() {
		targetDataLine.stop();
	}

}

package com.integralblue.commander.plugins.localaudio;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import com.integralblue.commander.api.AbstractPlugin;
import com.integralblue.commander.api.AudioData;
import com.integralblue.commander.api.Speaker;

import lombok.NonNull;

public class LocalSpeakerPlugin extends AbstractPlugin implements Speaker {
	private AudioFormat defaultAudioFormat;

	@Override
	public void initialize() throws Exception {
		super.initialize();
		defaultAudioFormat = new AudioFormat((float) config.getDouble("audioFormat.sampleRate"),
				config.getInt("audioFormat.sampleSizeInBits"), config.getInt("audioFormat.channels"),
				config.getBoolean("audioFormat.signed"), config.getBoolean("audioFormat.bigEndian"));
	}

	@Override
	public CompletionStage<Void> playAsync(AudioData audioData) {
		return playAsync(audioData.getAudioInputStream());
	}

	@Override
	public CompletionStage<Void> playAsync(@NonNull final AudioInputStream audioInputStreamParameter) {
		return CompletableFuture.completedFuture(audioInputStreamParameter).thenApply((audioInputStream) -> {
			if (AudioSystem.isLineSupported(new DataLine.Info(SourceDataLine.class, audioInputStream.getFormat()))) {
				return audioInputStream;
			} else {
				if (AudioSystem.isConversionSupported(defaultAudioFormat, audioInputStream.getFormat())) {
					return AudioSystem.getAudioInputStream(defaultAudioFormat, audioInputStream);
				} else {
					throw new IllegalArgumentException(
							"Cannot directly play or convert the given audioInputStream to a supported format. Format: "
									+ audioInputStream);
				}
			}
		}).thenCompose((audioInputStream) -> {
			try {
				final CompletableFuture<Void> future = new CompletableFuture<>();
				final Clip clip = (Clip) AudioSystem.getLine(new Line.Info(Clip.class));

				clip.addLineListener(new LineListener() {
					@Override
					public void update(LineEvent event) {
						if (event.getType() == LineEvent.Type.STOP) {
							clip.close();
							future.complete(null);
						}
					}
				});

				clip.open(audioInputStream);
				clip.start();
				return future;
			} catch (IOException | LineUnavailableException e) {
				throw new CompletionException(e);
			}
		});
	}

}

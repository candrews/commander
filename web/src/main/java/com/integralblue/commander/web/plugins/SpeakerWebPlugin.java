package com.integralblue.commander.web.plugins;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.CompletionStage;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import com.integralblue.commander.api.AudioData;
import com.integralblue.commander.api.Speaker;
import com.integralblue.commander.web.message.SpeakerRequestToClient;

import lombok.NonNull;
import lombok.SneakyThrows;

public class SpeakerWebPlugin extends AbstractWebPlugin implements Speaker {

	@Override
	@SneakyThrows
	public CompletionStage<Void> playAsync(@NonNull AudioData audioData) {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// TODO get the supported audio types from the client and only send one
		// of those types
		AudioSystem.write(audioData.getAudioInputStream(), Type.WAVE, baos);
		return sendRequestToClient(
				SpeakerRequestToClient.builder().mimeType("audio/wav").audio(baos.toByteArray()).build())
						.thenRun(() -> {
							// this thenRun is necessary to return the correct
							// type
							// (CompletionStage<Void>) as opposed to
							// CompletionStage<ResponseFromClient>
						});
	}

	@Override
	@SneakyThrows
	public CompletionStage<Void> playAsync(@NonNull AudioInputStream audioInputStream) {
		return playAsync(AudioData.fromAudioInputStream(audioInputStream));
	}

}

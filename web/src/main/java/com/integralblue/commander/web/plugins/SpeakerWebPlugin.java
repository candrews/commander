package com.integralblue.commander.web.plugins;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletionStage;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import com.integralblue.commander.api.AudioData;
import com.integralblue.commander.api.Speaker;
import com.integralblue.commander.web.message.SpeakerRequestToClient;

import lombok.NonNull;
import lombok.SneakyThrows;

public class SpeakerWebPlugin extends AbstractWebPlugin implements Speaker {
	private static final int BUFFER_SIZE = 4096;

	@Override
	@SneakyThrows
	public CompletionStage<Void> playAsync(@NonNull AudioData audioData) {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// TODO get the supported audio types from the client and only send one
		// of those types
		AudioInputStream ais = audioData.getAudioInputStream();
		
		// in order to write a WAV file, have to know how long the file is
		if(ais.getFrameLength()==AudioSystem.NOT_SPECIFIED){
			ais = AudioSystem.getAudioInputStream(Encoding.PCM_SIGNED, ais);
			ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
			copy(ais,baos2);
			ais = new AudioInputStream(new ByteArrayInputStream(baos2.toByteArray()), ais.getFormat(), baos2.size() / ais.getFormat().getFrameSize());
		}
		
		AudioSystem.write(ais, Type.WAVE, baos);
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


	/**
	 * Copy the contents of the given InputStream to the given OutputStream.
	 * Leaves both streams open when done.
	 *
	 * @param in
	 *            the InputStream to copy from
	 * @param out
	 *            the OutputStream to copy to
	 * @throws IOException
	 *             in case of I/O errors
	 */
	private static void copy(@NonNull InputStream in, @NonNull OutputStream out) throws IOException {
		byte[] buffer = new byte[BUFFER_SIZE];
		int bytesRead = -1;
		while ((bytesRead = in.read(buffer)) != -1) {
			out.write(buffer, 0, bytesRead);
		}
		out.flush();
	}
}

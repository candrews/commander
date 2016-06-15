package com.integralblue.commander.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import lombok.NonNull;
import lombok.Value;

@Value
public class AudioData {
	private static final int BUFFER_SIZE = 4096;

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

	/**
	 * Copy the data from the given {@link AudioInputStream} to a new
	 * {@link AudioData} instance. Leaves the {@link AudioInputStream} open when
	 * done.
	 * 
	 * @param audioInputStream
	 *            the InputStream to copy from
	 * @return
	 * @throws IOException
	 *             in case of I/O errors
	 */
	public static AudioData fromAudioInputStream(@NonNull AudioInputStream audioInputStream) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		copy(audioInputStream, baos);
		return new AudioData(baos.toByteArray(), audioInputStream.getFormat());
	}

	byte[] buffer;

	AudioFormat format;

	public AudioInputStream getAudioInputStream() {
		return new AudioInputStream(new ByteArrayInputStream(buffer), format,
				format.getFrameSize() == AudioSystem.NOT_SPECIFIED ? AudioSystem.NOT_SPECIFIED
						: (buffer.length / format.getFrameSize()));
	}
}

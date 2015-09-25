package com.integralblue.commander.web.plugins;

import java.util.concurrent.CompletionStage;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import com.integralblue.commander.api.AudioData;
import com.integralblue.commander.api.Microphone;

public class MicrophoneWebPlugin extends AbstractWebPlugin implements Microphone {

	@Override
	public AudioInputStream getStream() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public AudioInputStream getStream(AudioFormat audioFormat) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public CompletionStage<AudioData> getVoiceAsync(AudioFormat audioFormat) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void startRecording() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void stopRecording() {
		throw new RuntimeException("Not implemented");
	}

}

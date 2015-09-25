package com.integralblue.commander.plugins.freetts;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.sound.sampled.AudioInputStream;

import com.integralblue.commander.api.AbstractPlugin;
import com.integralblue.commander.api.SynthesisEngine;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

public class FreeTTSSynthesisPlugin extends AbstractPlugin implements SynthesisEngine {
	private Voice voice;

	@Override
	public void close() {
		voice.deallocate();
	}

	@Override
	public void initialize() throws Exception {
		super.initialize();
		VoiceManager voiceManager = VoiceManager.getInstance();
		voice = voiceManager.getVoice(config.getString("voice"));
		voice.allocate();
	}

	@Override
	public CompletionStage<Void> sayAsync(String textParameter) {
		return CompletableFuture.completedFuture(textParameter).thenCompose((text) -> {
			synchronized (voice) {
				final CompletableFuture<AudioInputStream> completableFuture = new CompletableFuture<>();
				voice.setAudioPlayer(new CompletableFutureAudioPlayer(completableFuture));
				voice.speak(text);
				return completableFuture;
			}
		}).thenCompose(manager.getSpeaker()::playAsync);
	}

}

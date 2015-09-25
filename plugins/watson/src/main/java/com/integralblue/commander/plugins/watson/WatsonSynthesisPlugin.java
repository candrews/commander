package com.integralblue.commander.plugins.watson;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.Voice;
import com.ibm.watson.developer_cloud.util.MediaType;
import com.integralblue.commander.api.SynthesisEngine;

public class WatsonSynthesisPlugin extends AbstractWatsonPlugin implements SynthesisEngine {
	private static final AudioFormat WATSON_AUDIO_FORMAT = new AudioFormat(22050, 16, 1, true, false);
	private static final String MEDIA_TYPE = MediaType.AUDIO_WAV;
	private TextToSpeech textToSpeech;
	private Voice voice;

	@Override
	public void initialize() throws Exception {
		super.initialize();
		textToSpeech = new TextToSpeech();
		textToSpeech.setUsernameAndPassword(username, password);
		final String voiceName = config.getString("voice");
		final List<Voice> voices = textToSpeech.getVoices();
		for (Voice voice : voices) {
			if (voiceName.equals(voice.getName())) {
				this.voice = voice;
			}
		}
		if (this.voice == null) {
			throw new IllegalArgumentException(
					"Configuration specified voice name '" + voiceName + "' is not supported. Supported voices are: "
							+ voices.stream().map(Voice::getName).collect(Collectors.joining(",")));
		}
	}

	@Override
	public CompletionStage<Void> sayAsync(String text) {
		return CompletableFuture.supplyAsync(() -> {
			return new AudioInputStream(textToSpeech.synthesize(text, voice, MEDIA_TYPE), WATSON_AUDIO_FORMAT,
					AudioSystem.NOT_SPECIFIED);
		}).thenCompose(manager.getSpeaker()::playAsync);
	}
}

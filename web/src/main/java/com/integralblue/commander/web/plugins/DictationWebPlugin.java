package com.integralblue.commander.web.plugins;

import java.io.ByteArrayInputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.sound.sampled.AudioSystem;

import com.integralblue.commander.api.AudioData;
import com.integralblue.commander.api.DictationEngine;
import com.integralblue.commander.api.DictationResult;
import com.integralblue.commander.web.message.DictationRequestToClient;
import com.integralblue.commander.web.message.DictationResponseFromClient;

public class DictationWebPlugin extends AbstractWebPlugin implements DictationEngine {
	private DictationEngine dictationEngine;

	@Override
	public void initialize() throws Exception {
		dictationEngine = manager.getPlugin(config.getString("dictationEngine"), DictationEngine.class);
		super.initialize();
	}

	@Override
	public CompletionStage<DictationResult> listenForDictationAsync() {
		return sendRequestToClient(DictationRequestToClient.builder().build()).thenCompose((response) -> {
			DictationResponseFromClient dictationResponseFromClient = (DictationResponseFromClient) response;
			if (dictationResponseFromClient.getText() == null && dictationResponseFromClient.getAudio() == null) {
				throw new IllegalArgumentException("text or audio must be specified");
			}

			AudioData audioData;
			if (dictationResponseFromClient.getAudio() == null) {
				audioData = null;
			} else {
				try {
					audioData = AudioData.fromAudioInputStream(AudioSystem
							.getAudioInputStream(new ByteArrayInputStream(dictationResponseFromClient.getAudio())));
				} catch (Exception e) {
					throw new IllegalArgumentException("Invalid audio data", e);
				}
			}

			if (dictationResponseFromClient.getText() == null) {
				// The client sent audio, but not text, send the audio to
				// another DictationEngine
				return dictationEngine.listenForDictationAsync(audioData);
				//return dictationEngine.listenForDictationAsync(audioData);
			} else {
				return CompletableFuture.completedFuture(DictationResult.builder().audioData(audioData)
						.text(dictationResponseFromClient.getText()).build());
			}
		});
	}

	@Override
	public CompletionStage<DictationResult> listenForDictationAsync(AudioData audioData) {
		return dictationEngine.listenForDictationAsync(audioData);
	}

}

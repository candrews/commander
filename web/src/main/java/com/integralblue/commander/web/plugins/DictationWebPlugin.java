package com.integralblue.commander.web.plugins;

import java.util.concurrent.CompletionStage;

import com.integralblue.commander.api.AudioData;
import com.integralblue.commander.api.DictationEngine;
import com.integralblue.commander.api.DictationResult;
import com.integralblue.commander.web.message.DictationRequestToClient;
import com.integralblue.commander.web.message.DictationResponseFromClient;

public class DictationWebPlugin extends AbstractWebPlugin implements DictationEngine {

	@Override
	public void initialize() throws Exception {
		super.initialize();
	}

	@Override
	public CompletionStage<DictationResult> listenForDictationAsync() {
		return sendRequestToClient(DictationRequestToClient.builder().build()).thenApply((response) -> {
			DictationResponseFromClient dictationResponseFromClient = (DictationResponseFromClient) response;
			// TODO if the client sent audio, include that in the
			// DictationResult

			// TODO if the client send audio, but not text, send the audio to
			// another DictationEngine
			return DictationResult.builder().text(dictationResponseFromClient.getText()).build();
		});
	}

	@Override
	public CompletionStage<DictationResult> listenForDictationAsync(AudioData audioData) {
		// TODO send the audio to another DictationEngine
		throw new RuntimeException("Not yet implemented");
	}

}

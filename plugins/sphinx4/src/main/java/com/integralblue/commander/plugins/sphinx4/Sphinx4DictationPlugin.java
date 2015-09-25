package com.integralblue.commander.plugins.sphinx4;

import java.util.concurrent.CompletionStage;

import com.integralblue.commander.api.AudioData;
import com.integralblue.commander.api.DictationEngine;
import com.integralblue.commander.api.DictationResult;

public class Sphinx4DictationPlugin extends AbstractSphinx4RecognitionPlugin implements DictationEngine {

	@Override
	public CompletionStage<DictationResult> listenForDictationAsync() {
		return manager.getMicrophone().getVoiceAsync(audioFormat).thenCompose(this::listenForDictationAsync);
	}

	@Override
	public CompletionStage<DictationResult> listenForDictationAsync(AudioData audioData) {
		return listenForText(audioData).thenApply((sphinxResult) -> DictationResult.builder()
				.text(sphinxResult.getText()).audioData(sphinxResult.getAudioData()).build());
	}

}

package com.integralblue.commander.plugins.marytts;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import com.integralblue.commander.api.AbstractPlugin;
import com.integralblue.commander.api.SynthesisEngine;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.exceptions.SynthesisException;

public class MaryTTSSynthesisPlugin extends AbstractPlugin implements SynthesisEngine {
	private MaryInterface marytts;

	@Override
	public void initialize() throws Exception {
		super.initialize();
		marytts = new LocalMaryInterface();
		String voiceName = config.getString("voice");
		if (marytts.getAvailableVoices().contains(voiceName)) {
			marytts.setVoice(voiceName);
		} else {
			throw new IllegalArgumentException(
					"Configuration specified voice name '" + voiceName + "' is not supported. Supported voices are: "
							+ marytts.getAvailableVoices().stream().collect(Collectors.joining(",")));
		}
	}

	@Override
	public CompletionStage<Void> sayAsync(String textParameter) {
		return CompletableFuture.completedFuture(textParameter).thenApply((text) -> {
			try {
				return marytts.generateAudio(text);
			} catch (SynthesisException e) {
				throw new RuntimeException(e);
			}
		}).thenCompose(manager.getSpeaker()::playAsync);
	}

}

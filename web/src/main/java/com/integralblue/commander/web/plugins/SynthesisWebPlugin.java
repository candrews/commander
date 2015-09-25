package com.integralblue.commander.web.plugins;

import java.util.concurrent.CompletionStage;

import com.integralblue.commander.api.SynthesisEngine;
import com.integralblue.commander.web.message.SynthesisRequestToClient;

public class SynthesisWebPlugin extends AbstractWebPlugin implements SynthesisEngine {

	/**
	 * Send text to the client and have it synthesize? If false, synthesize on
	 * the server, sending audio data to the client to play
	 */
	private boolean synthesizeOnClient;

	private SynthesisEngine synthesisEngine;

	@Override
	public void initialize() throws Exception {
		super.initialize();
		synthesizeOnClient = config.getBoolean("synthesizeOnClient");
		synthesisEngine = manager.getPlugin(config.getString("synthesisEngine"), SynthesisEngine.class);
	}

	@Override
	public CompletionStage<Void> sayAsync(String text) {
		if (synthesizeOnClient) {
			return synthesizeOnClient(text);
		} else {
			return synthesizeOnServer(text);
		}
	}

	private CompletionStage<Void> synthesizeOnClient(String text) {
		final SynthesisRequestToClient synthesisMessage = SynthesisRequestToClient.builder().text(text).build();
		return sendRequestToClient(synthesisMessage).thenRun(() -> {
			// this thenRun is necessary to return the correct type
			// (CompletionStage<Void>) as opposed to
			// CompletionStage<ResponseFromClient>
		});
	}

	private CompletionStage<Void> synthesizeOnServer(String text) {
		return synthesisEngine.sayAsync(text);
	}
}

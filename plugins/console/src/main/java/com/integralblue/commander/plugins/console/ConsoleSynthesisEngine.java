package com.integralblue.commander.plugins.console;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.integralblue.commander.api.AbstractPlugin;
import com.integralblue.commander.api.SynthesisEngine;

public class ConsoleSynthesisEngine extends AbstractPlugin implements SynthesisEngine {

	@Override
	public CompletionStage<Void> sayAsync(String text) {
		return CompletableFuture.completedFuture(text).thenAccept(System.out::println);
	}

}

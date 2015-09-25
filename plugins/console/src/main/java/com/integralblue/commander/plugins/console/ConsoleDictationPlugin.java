package com.integralblue.commander.plugins.console;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.integralblue.commander.api.AbstractPlugin;
import com.integralblue.commander.api.AudioData;
import com.integralblue.commander.api.DictationEngine;
import com.integralblue.commander.api.DictationResult;

public class ConsoleDictationPlugin extends AbstractPlugin implements DictationEngine {
	Scanner scanner;

	@Override
	public void close() {
		scanner.close();
	}

	@Override
	public void initialize() throws Exception {
		super.initialize();
		scanner = new Scanner(System.in);
	}

	@Override
	public CompletionStage<DictationResult> listenForDictationAsync() {
		// TODO use some kind of async IO instead of blocking IO on a different
		// thread
		return CompletableFuture.supplyAsync(() -> {
			final String input = scanner.nextLine();
			return DictationResult.builder().text(input).build();
		});
	}

	@Override
	public CompletionStage<DictationResult> listenForDictationAsync(AudioData audioData) {
		throw new RuntimeException(
				"ConsoleDictationPlugin doesn't actually process audio, so it cannot listen to a specific audioInputStream");
	}

}

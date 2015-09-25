package com.integralblue.commander.plugins.adapters;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import com.integralblue.commander.Manager;
import com.integralblue.commander.api.AbstractPlugin;
import com.integralblue.commander.api.GrammarEngine;
import com.integralblue.commander.api.JsgfRule;
import com.integralblue.commander.api.KeywordEngine;

public class GrammarToKeywordRecognitionAdapter extends AbstractPlugin implements KeywordEngine {
	private GrammarEngine grammarEngine;

	@Override
	public void initialize() throws Exception {
		super.initialize();
		grammarEngine = manager.getPlugin(config.getString("grammarEngine"), GrammarEngine.class);
	}

	@Override
	public CompletionStage<String> listenForKeywordsAsync(Collection<String> keywords) {
		return grammarEngine
				.listenForGrammarAsync(
						Collections
								.singleton(
										JsgfRule.builder().name("KEYWORD")
												.jsgf(keywords.stream().map(s -> "(" + Manager.escapeJsgf(s) + ")")
														.collect(Collectors.joining("|")))
												.build()))
				.thenCompose((result) -> {
					if (result.isUnknown()) {
						return listenForKeywordsAsync(keywords);
					} else {
						return CompletableFuture.completedFuture(result.getText());
					}
				});
	}
}

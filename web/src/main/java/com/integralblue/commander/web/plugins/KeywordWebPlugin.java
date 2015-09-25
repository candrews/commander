package com.integralblue.commander.web.plugins;

import java.util.Collection;
import java.util.concurrent.CompletionStage;

import com.integralblue.commander.api.KeywordEngine;

public class KeywordWebPlugin extends AbstractWebPlugin implements KeywordEngine {

	@Override
	public CompletionStage<String> listenForKeywordsAsync(Collection<String> keywords) {
		throw new RuntimeException("Not yet implemented");
	}

}

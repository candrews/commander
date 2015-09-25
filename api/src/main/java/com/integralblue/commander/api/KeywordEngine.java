package com.integralblue.commander.api;

import java.util.Collection;
import java.util.concurrent.CompletionStage;

public interface KeywordEngine extends RecognitionEngine, Plugin {
	/**
	 * Listen for keywords.
	 *
	 * This method need not be thread safe - for a given instance, this method
	 * will only be called by one thread at a time.
	 *
	 * @param keywords
	 * @return
	 */
	CompletionStage<String> listenForKeywordsAsync(Collection<String> keywords);
}

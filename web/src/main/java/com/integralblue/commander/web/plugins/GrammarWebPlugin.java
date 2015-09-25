package com.integralblue.commander.web.plugins;

import java.util.Collection;
import java.util.concurrent.CompletionStage;

import com.integralblue.commander.api.AudioData;
import com.integralblue.commander.api.GrammarEngine;
import com.integralblue.commander.api.GrammarResult;
import com.integralblue.commander.api.JsgfRule;

public class GrammarWebPlugin extends AbstractWebPlugin implements GrammarEngine {

	@Override
	public CompletionStage<GrammarResult> listenForGrammarAsync(Collection<JsgfRule> rules) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public CompletionStage<GrammarResult> listenForGrammarAsync(Collection<JsgfRule> rules, AudioData audioData) {
		throw new RuntimeException("Not implemented");
	}

}

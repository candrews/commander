package com.integralblue.commander.plugins.repeat;

import com.integralblue.commander.Manager.DictationConfiguration;
import com.integralblue.commander.api.AbstractPlugin;

public class RepeatPlugin extends AbstractPlugin {

	@Override
	public void initialize() {
		manager.onMainMenuOption("repeat after me", (mainMenuMenuController, mainMenuGrammarResult) -> {
			manager.dictate(DictationConfiguration.builder().prompt(() -> manager.say("Say something"))
					.consumer((mc, r) -> manager.say(r.getText())).build());
		});
	}

}

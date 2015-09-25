package com.integralblue.commander.plugins.mainmenuprompt;

import com.integralblue.commander.api.AbstractPlugin;

public class MainMenuPromptPlugin extends AbstractPlugin {

	@Override
	public void initialize() throws Exception {
		super.initialize();
		manager.onMainMenu(mc -> mc.withPrompt(() -> {
			manager.say("Main menu");
			mc.getPrompt().run();
		}));
	}

}

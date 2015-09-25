package com.integralblue.commander.plugins.repeatunknown;

import com.integralblue.commander.api.AbstractPlugin;

public class RepeatUnknownPlugin extends AbstractPlugin {

	@Override
	public void initialize() throws Exception {
		super.initialize();
		manager.onAnyMenu(menuConfiguration -> menuConfiguration.withOnUnknown((menuController, result) -> {
			manager.say("Sorry, I didn't understand what you said");
			menuController.repeatMenu();
			menuConfiguration.getOnUnknown().accept(menuController, result);
		}));
		manager.onAnyDictate(
				dictationConfiguration -> dictationConfiguration.withOnUnknown((menuController, result) -> {
					manager.say("Sorry, I didn't understand what you said");
					menuController.awaitCompletion(menuController.repeatMenuAsync());
					dictationConfiguration.getOnUnknown().accept(menuController, result);
				}));
	}

}

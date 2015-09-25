package com.integralblue.commander.plugins.weather;

import com.integralblue.commander.Manager.MenuConfiguration;
import com.integralblue.commander.api.AbstractPlugin;

public class WeatherPlugin extends AbstractPlugin {

	@Override
	public void initialize() throws Exception {
		super.initialize();
		manager.onMainMenuOption("weather", (mainMenuController, mainMenuResult) -> {
			mainMenuController.awaitCompletion(
					manager.menuAsync(MenuConfiguration.builder().prompt(() -> manager.say("What city?"))
							.jsgfToConsumer("Boston", (mc, result) -> manager.sayAsync("cold")).build()));
		});
	}

}

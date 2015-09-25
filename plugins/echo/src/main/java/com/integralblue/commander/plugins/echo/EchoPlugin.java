package com.integralblue.commander.plugins.echo;

import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.integralblue.commander.Manager.MenuController;
import com.integralblue.commander.api.AbstractPlugin;
import com.integralblue.commander.api.GrammarResult;

public class EchoPlugin extends AbstractPlugin {

	@Override
	public void initialize() throws Exception {
		super.initialize();
		manager.onAnyDictate(dictationConfiguration -> {
			return dictationConfiguration.withConsumer((menuController, result) -> {
				manager.say("You said: " + result.getText());
				dictationConfiguration.getConsumer().accept(menuController, result);
			});
		});
		manager.onAnyMenu(menuConfiguration -> {
			BiConsumer<MenuController, GrammarResult> sayResult = (menuController, result) -> manager
					.say("You said: " + result.getText());
			return menuConfiguration.withJsgfToConsumers(menuConfiguration.getJsgfToConsumers().entrySet().stream()
					.collect(Collectors.toMap(entry -> entry.getKey(), entry -> sayResult.andThen(entry.getValue()))));
		});
	}
}

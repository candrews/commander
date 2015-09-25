package com.integralblue.commander.plugins.what;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import com.integralblue.commander.Manager.MenuController;
import com.integralblue.commander.api.AbstractPlugin;
import com.integralblue.commander.api.GrammarResult;

/**
 * Whenever you say "What" the prompt for input will be repeated
 *
 */
public class WhatPlugin extends AbstractPlugin {

	@Override
	public void initialize() throws Exception {
		super.initialize();

		final String whatJsgf = config.getString("whatJsgf");

		manager.onAnyMenu(mc -> {
			Map<String, BiConsumer<MenuController, GrammarResult>> jsgfToFunction = new HashMap<>(
					mc.getJsgfToConsumers());
			jsgfToFunction.put(whatJsgf, (menuController, result) -> {
				menuController.repeatMenu();
			});
			return mc.withJsgfToConsumers(jsgfToFunction);
		});
		manager.onAnyDictate(dc -> {
			return dc.withConsumer((menuController, result) -> {
				if (whatJsgf.equalsIgnoreCase(result.getText())) {
					menuController.repeatMenu();
				} else {
					dc.getConsumer().accept(menuController, result);
				}
			});
		});
	}

}

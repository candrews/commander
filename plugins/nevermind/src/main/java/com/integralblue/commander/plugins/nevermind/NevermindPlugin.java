package com.integralblue.commander.plugins.nevermind;

import java.util.List;
import java.util.stream.Collectors;

import com.integralblue.commander.api.AbstractPlugin;

public class NevermindPlugin extends AbstractPlugin {

	@Override
	public void initialize() throws Exception {
		super.initialize();

		final List<String> nevermindPhrases = config.getStringList("nevermindPhrases").stream()
				.map(s -> s.toLowerCase()).collect(Collectors.toList());

		manager.onAnyMenu(mc -> {
			return mc.toBuilder()
					.jsgfToConsumer(
							nevermindPhrases.stream().map(entry -> "(" + entry + ")").collect(Collectors.joining("|")),
							(menuController, result) -> {
				// doing nothing will return back to keyword listening
			}).build();
		});
		manager.onAnyDictate(dc -> {
			return dc.withConsumer((menuController, result) -> {
				if (nevermindPhrases.contains(result.getText().toLowerCase())) {
					// doing nothing will return back to keyword listening
				} else {
					dc.getConsumer().accept(menuController, result);
				}
			});
		});
	}

}

package com.integralblue.commander.plugins.quit;

import com.integralblue.commander.api.AbstractPlugin;

public class QuitPlugin extends AbstractPlugin {
	@Override
	public void initialize() throws Exception {
		super.initialize();
		manager.onMainMenuOption(config.getString("jsgf"), (mc, r) -> manager.quit());
	}

}

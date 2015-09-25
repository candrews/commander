package com.integralblue.commander.plugins.watson;

import com.integralblue.commander.api.AbstractPlugin;

public class AbstractWatsonPlugin extends AbstractPlugin {

	protected String username;
	protected String password;

	@Override
	public void initialize() throws Exception {
		super.initialize();
		if (!config.hasPath("username") || !config.hasPath("password")) {
			throw new IllegalArgumentException(
					"username and/or password is not set in the configuration - please provide Watson credentials. Sign up at https://console.ng.bluemix.net/pricing/");
		}
		username = config.getString("username");
		password = config.getString("password");
	}

}

package com.integralblue.commander.api;

import com.typesafe.config.Config;

public abstract class AbstractLoader implements Loader {
	protected Config config;

	@Override
	public void initialize() throws Exception {

	}

	@Override
	public final void setConfig(Config config) {
		this.config = config;
	}

}

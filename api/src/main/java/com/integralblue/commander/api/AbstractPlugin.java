package com.integralblue.commander.api;

import com.integralblue.commander.Manager;
import com.typesafe.config.Config;

import lombok.NonNull;

public abstract class AbstractPlugin implements Plugin {
	protected Manager manager;
	protected Config config;

	@Override
	public void close() throws Exception {
	}

	@Override
	public void initialize() throws Exception {
	}

	@Override
	public final void setConfig(@NonNull Config config) {
		this.config = config;
	}

	@Override
	public final void setManager(@NonNull Manager manager) {
		this.manager = manager;
	}

}

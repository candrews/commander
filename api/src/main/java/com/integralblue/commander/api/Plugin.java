package com.integralblue.commander.api;

import com.integralblue.commander.Manager;
import com.typesafe.config.Config;

public interface Plugin extends AutoCloseable {

	/**
	 * Called exactly once when the plugin is initialized.
	 * {@link #setManager(Manager)} and {@link #setConfig(Config)} will have
	 * already been called. Use the given {@link Manager} and
	 * {@link #setConfig(Config)} to implement the plugin.
	 *
	 * @throws Exception
	 */
	void initialize() throws Exception;

	void setConfig(Config config);

	void setManager(Manager manager);

}
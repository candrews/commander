package com.integralblue.commander.api;

import com.typesafe.config.Config;

public interface Loader {
	/**
	 * Get the classloader using the specified parent classloader
	 *
	 * @param parameters
	 *            the parameters specified for this loader instance. If no
	 *            parameters were specified, this will be null. If empty
	 *            parameters were specified, this will be the empty string.
	 * @param parent
	 * @return
	 */
	ClassLoader getClassLoader(String parameters, ClassLoader parent) throws Exception;

	void initialize() throws Exception;

	void setConfig(Config config);
}

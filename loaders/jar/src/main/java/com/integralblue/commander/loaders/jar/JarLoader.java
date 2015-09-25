package com.integralblue.commander.loaders.jar;

import java.net.URL;
import java.net.URLClassLoader;

import com.integralblue.commander.api.AbstractLoader;

public class JarLoader extends AbstractLoader {

	@Override
	public ClassLoader getClassLoader(String parameters, ClassLoader parent) throws Exception {
		if (parameters == null || parameters.isEmpty()) {
			throw new IllegalArgumentException(
					"Error loading classes with the jar loader. The parameter must be specified to specify the URL of the jar to load");
		}
		URL url = new URL(parameters);
		return URLClassLoader.newInstance(new URL[] { url }, parent);
	}

}

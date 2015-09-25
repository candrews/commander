package com.integralblue.commander.loaders.groovy;

import java.net.URI;

import com.integralblue.commander.api.AbstractLoader;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;

public class GroovyLoader extends AbstractLoader {

	@Override
	public ClassLoader getClassLoader(String parameters, ClassLoader parent) throws Exception {
		if (parameters == null || parameters.isEmpty()) {
			throw new IllegalArgumentException(
					"Error loading classes with the jar loader. The parameter must be specified to specify the URL of the jar to load");
		}
		GroovyClassLoader classLoader = new GroovyClassLoader(parent);

		// the class returned by parseClass is intentionally not used
		// the class has to be loaded by name later - parseClass is just used to
		// get the class into the classloader
		classLoader.parseClass(new GroovyCodeSource(URI.create(parameters)));

		return classLoader;
	}

}

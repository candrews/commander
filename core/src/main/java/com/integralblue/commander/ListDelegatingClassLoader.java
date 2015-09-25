package com.integralblue.commander;

/**
 * ClassLoader implementation that delegates to a series of a classloaders
 *
 */
public class ListDelegatingClassLoader extends ClassLoader {

	private final ClassLoader[] classLoaders;

	public ListDelegatingClassLoader(ClassLoader[] classLoaders) {
		super(classLoaders[0]);
		if (classLoaders.length == 0) {
			throw new IllegalArgumentException("Must specify at least one classloader to delegate to.");
		}
		this.classLoaders = classLoaders;
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		try {
			return super.loadClass(name, resolve);
		} catch (ClassNotFoundException cne) {
			for (int i = 1; i < classLoaders.length; i++) {
				try {
					return classLoaders[i].loadClass(name);
				} catch (ClassNotFoundException e) {
					// do nothing, just try the next classloader
				}
			}
			throw cne;
		}
	}

}

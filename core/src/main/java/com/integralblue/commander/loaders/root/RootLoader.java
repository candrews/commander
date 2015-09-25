package com.integralblue.commander.loaders.root;

import com.integralblue.commander.api.AbstractLoader;

public class RootLoader extends AbstractLoader {

	@Override
	public ClassLoader getClassLoader(String parameters, ClassLoader parent) {
		return parent;
	}

}

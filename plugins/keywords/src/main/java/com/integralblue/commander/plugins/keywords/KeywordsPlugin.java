package com.integralblue.commander.plugins.keywords;

import com.integralblue.commander.api.AbstractPlugin;

public class KeywordsPlugin extends AbstractPlugin {

	@Override
	public void initialize() throws Exception {
		super.initialize();
		config.getStringList("keywords").stream().map(s -> s.trim()).forEach(manager::addKeyword);
	}

}

package com.integralblue.commander.plugins.logsay;

import com.integralblue.commander.api.AbstractPlugin;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogSayPlugin extends AbstractPlugin {

	@Override
	public void initialize() throws Exception {
		super.initialize();
		manager.onSay(next -> text -> {
			log.info(text);
			next.accept(text);
		});
	}

}

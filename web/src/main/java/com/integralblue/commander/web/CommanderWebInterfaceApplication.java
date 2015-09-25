package com.integralblue.commander.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;

@SpringBootApplication
public class CommanderWebInterfaceApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(CommanderWebInterfaceApplication.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return super.configure(application).sources(CommanderWebInterfaceApplication.class);
	}
}

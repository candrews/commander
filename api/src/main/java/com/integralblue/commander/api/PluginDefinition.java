package com.integralblue.commander.api;

import com.typesafe.config.Config;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Wither;

@Builder(toBuilder = true)
@Value
@Wither
public class PluginDefinition {
	@NonNull
	Class<? extends Plugin> clazz;

	@NonNull
	Config config;
}

package com.integralblue.commander;

import com.typesafe.config.Config;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Builder
@Value
class LoaderDefinition {
	@NonNull
	String loaderLoader;

	@NonNull
	String className;

	@NonNull
	Config config;
}
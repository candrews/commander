package com.integralblue.commander.api;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Wither;

@Value
@Wither
@Builder(toBuilder = true)
public class JsgfRule {
	public static class JsgfRuleBuilder {
		boolean isPublic = true;
	}

	@NonNull
	String name;

	boolean isPublic;

	@NonNull
	String jsgf;
}
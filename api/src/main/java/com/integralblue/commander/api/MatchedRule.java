package com.integralblue.commander.api;

import java.util.List;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Wither;

@Value
@Wither
@Builder(toBuilder = true)
public class MatchedRule {
	@NonNull
	String name;

	@NonNull
	List<String> tags;
}

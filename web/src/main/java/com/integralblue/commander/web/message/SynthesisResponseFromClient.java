package com.integralblue.commander.web.message;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonDeserialize(builder = SynthesisResponseFromClient.SynthesisResponseFromClientBuilder.class)
public class SynthesisResponseFromClient extends ResponseFromClient {
	@JsonPOJOBuilder(withPrefix = "")
	public static final class SynthesisResponseFromClientBuilder {

	}

	@NonNull
	String id;

}

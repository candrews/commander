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
@JsonDeserialize(builder = SpeakerResponseFromClient.SpeakerResponseFromClientBuilder.class)
public class SpeakerResponseFromClient extends ResponseFromClient {
	@JsonPOJOBuilder(withPrefix = "")
	public static final class SpeakerResponseFromClientBuilder {

	}

	@NonNull
	String id;

}

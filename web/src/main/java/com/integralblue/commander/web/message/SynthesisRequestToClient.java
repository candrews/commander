package com.integralblue.commander.web.message;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

/**
 * Requests that the client synthesize the given text
 */
@Value
@EqualsAndHashCode(callSuper = true)
@Builder
public class SynthesisRequestToClient extends RequestToClient {
	@NonNull
	String text;
}

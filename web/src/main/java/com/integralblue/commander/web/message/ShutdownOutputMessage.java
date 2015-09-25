package com.integralblue.commander.web.message;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * Tells the client that the brain they were using has shutdown
 *
 */
@Value
@Builder
public class ShutdownOutputMessage {
	@NonNull
	String message;
}

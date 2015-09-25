package com.integralblue.commander.web.message;

import lombok.Builder;
import lombok.Value;

/**
 * Tell the client to display something on the console Note that this class does
 * not implement {@link RequestToClient} because the client doesn't need to
 * respond to this message.
 */
@Value
@Builder
public class ConsoleOutputMessage {
	String text;
}

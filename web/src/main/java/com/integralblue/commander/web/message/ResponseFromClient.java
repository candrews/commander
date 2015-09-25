package com.integralblue.commander.web.message;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Message that indicates that a {@link RequestToClient} has completed on the
 * client. {@link #getId()} matches {@link ResponseFromClient#getId()}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class ResponseFromClient {
	public abstract String getId();
}

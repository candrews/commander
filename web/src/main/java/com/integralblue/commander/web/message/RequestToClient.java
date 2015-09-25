package com.integralblue.commander.web.message;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

/**
 * Represents a message which the client must signal when it's done handling.
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class RequestToClient {
	@NonNull
	final String id = UUID.randomUUID().toString();
}

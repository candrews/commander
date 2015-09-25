package com.integralblue.commander.web;

import java.util.HashSet;
import java.util.Set;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.user.UserDestinationResolver;
import org.springframework.messaging.simp.user.UserDestinationResult;
import org.springframework.util.Assert;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SessionIdUserDestinationResolver implements UserDestinationResolver {

	/**
	 * A temporary placeholder for a parsed source "user" destination.
	 */
	private static class ParseResult {

		private final String actualDestination;

		private final String subscribeDestination;

		private final String sessionId;

		public ParseResult(@NonNull String actualDest, @NonNull String subscribeDest, @NonNull String sessionId) {
			this.actualDestination = actualDest;
			this.subscribeDestination = subscribeDest;
			this.sessionId = sessionId;
		}

		public String getActualDestination() {
			return this.actualDestination;
		}

		public String getSessionId() {
			return sessionId;
		}

		public String getSubscribeDestination() {
			return this.subscribeDestination;
		}
	}

	private String prefix = "/user/";

	protected boolean checkDestination(String destination, String requiredPrefix) {
		return destination.startsWith(requiredPrefix);
	}

	/**
	 * This method determines how to translate the source "user" destination to
	 * an actual target destination for the given active user session.
	 *
	 * @param sourceDestination
	 *            the source destination from the input message.
	 * @param actualDestination
	 *            a subset of the destination without any user prefix.
	 * @param sessionId
	 *            the id of an active user session, never {@code null}.
	 * @param user
	 *            the target user, possibly {@code null}, e.g if not
	 *            authenticated.
	 * @return a target destination, or {@code null} if none
	 */
	protected String getTargetDestination(String sourceDestination, String actualDestination, String sessionId) {

		return actualDestination + "-user" + sessionId;
	}

	private ParseResult parse(Message<?> message) {
		MessageHeaders headers = message.getHeaders();
		String destination = SimpMessageHeaderAccessor.getDestination(headers);
		if (destination == null || !checkDestination(destination, this.prefix)) {
			return null;
		}
		SimpMessageType messageType = SimpMessageHeaderAccessor.getMessageType(headers);
		final String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
		if (SimpMessageType.SUBSCRIBE.equals(messageType) || SimpMessageType.UNSUBSCRIBE.equals(messageType)) {
			if (sessionId == null) {
				log.error("No session id. Ignoring " + message);
				return null;
			}
			int prefixEnd = this.prefix.length() - 1;
			String actualDestination = destination.substring(prefixEnd);
			return new ParseResult(actualDestination, destination, sessionId);
		} else if (SimpMessageType.MESSAGE.equals(messageType)) {
			int prefixEnd = this.prefix.length();
			int userEnd = destination.indexOf('/', prefixEnd);
			Assert.isTrue(userEnd > 0, "Expected destination pattern \"/user/{userId}/**\"");
			String actualDestination = destination.substring(userEnd);
			String subscribeDestination = this.prefix.substring(0, prefixEnd - 1) + actualDestination;
			String sessionIdFromDestination = destination.substring(prefixEnd, userEnd);
			return new ParseResult(actualDestination, subscribeDestination, sessionIdFromDestination);
		} else {
			return null;
		}
	}

	@Override
	public UserDestinationResult resolveDestination(Message<?> message) {
		String sourceDestination = SimpMessageHeaderAccessor.getDestination(message.getHeaders());
		ParseResult parseResult = parse(message);
		if (parseResult == null) {
			return null;
		}
		Set<String> targetSet = new HashSet<String>();
		String actualDestination = parseResult.getActualDestination();
		String sessionId = parseResult.getSessionId();
		String targetDestination = getTargetDestination(sourceDestination, actualDestination, sessionId);
		if (targetDestination != null) {
			targetSet.add(targetDestination);
		}
		String subscribeDestination = parseResult.getSubscribeDestination();
		return new UserDestinationResult(sourceDestination, targetSet, subscribeDestination, sessionId);
	}
}

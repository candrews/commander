package com.integralblue.commander.web.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.integralblue.commander.Brain;
import com.integralblue.commander.Commander;
import com.integralblue.commander.web.message.RequestToClient;
import com.integralblue.commander.web.message.ResponseFromClient;
import com.integralblue.commander.web.message.ShutdownOutputMessage;
import com.integralblue.commander.web.plugins.AbstractWebPlugin;
import com.integralblue.commander.web.properties.BrainProperties;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BrainService implements ApplicationContextAware {

	@Component
	public static class SessionDisconnectListener implements ApplicationListener<SessionDisconnectEvent> {
		@Autowired
		private BrainService brainService;

		@Override
		public void onApplicationEvent(SessionDisconnectEvent event) {
			brainService.onApplicationEvent(event);
		}

	}

	@Data
	@FieldDefaults(level = AccessLevel.PRIVATE)
	private static class SessionInfo {
		final Map<String, CompletableFuture<ResponseFromClient>> completableIdToCompletableFutures = new ConcurrentHashMap<>();
		@NonNull
		Brain brain;
	}

	@Autowired
	private SimpMessagingTemplate template;

	@Autowired
	private BrainProperties brainProperties;

	final ConcurrentMap<String, SessionInfo> sessionIdToSessionInfo = new ConcurrentHashMap<>();

	public void complete(@NonNull String sessionId, @NonNull ResponseFromClient completedMessage) {
		final SessionInfo sessionInfo = sessionIdToSessionInfo.get(sessionId);
		if (sessionInfo == null) {
			throw new IllegalStateException(
					"Try to register a completion message for a session that doesn't exist. Session id: " + sessionId);
		} else {
			final String id = completedMessage.getId();
			final Map<String, CompletableFuture<ResponseFromClient>> completableIdToCompletableFutures = sessionInfo
					.getCompletableIdToCompletableFutures();
			final CompletableFuture<ResponseFromClient> completableFuture = completableIdToCompletableFutures
					.get(completedMessage.getId());
			if (completableFuture == null) {
				throw new IllegalStateException(
						"No completable future found in session " + sessionId + " for id " + id);
			} else {
				completableFuture.complete(completedMessage);
			}
		}
	}

	/**
	 * Signals that a give message has finished being processed by the client
	 *
	 * @param sessionId
	 * @param completedMessage
	 */
	public void completedMessage(@NonNull String sessionId, @NonNull ResponseFromClient completedMessage) {
		SessionInfo sessionInfo = sessionIdToSessionInfo.get(sessionId);
		if (sessionInfo == null) {
			throw new IllegalStateException(
					"Received a CompletedMessage for a session that doesn't exist. Session id: " + sessionId);
		} else {
			final CompletableFuture<ResponseFromClient> completableFuture = sessionInfo
					.getCompletableIdToCompletableFutures().get(completedMessage.getId());
			if (completableFuture == null) {
				throw new IllegalStateException(
						"Received a CompletedMessage with an ID that doesn't exist. CompletedMessage id: "
								+ completedMessage.getId() + " Session id: " + sessionId);
			} else {
				completableFuture.complete(completedMessage);
			}
		}
	}

	private void endSession(@NonNull String sessionId) {
		final SessionInfo sessionInfo = sessionIdToSessionInfo.get(sessionId);
		if (sessionInfo != null) {
			// found a Brain associated with this session
			final Brain brain = sessionInfo.getBrain();
			final CompletableFuture<Void> brainCompletableFuture = brain.getCompletableFuture();
			List<CompletableFuture<?>> allCompletableFutures = new ArrayList<>();
			if (brainCompletableFuture != null) {
				allCompletableFutures.add(brainCompletableFuture);
			}
			allCompletableFutures.addAll(sessionInfo.getCompletableIdToCompletableFutures().values());

			// cancel all futures to make sure they don't stick around
			allCompletableFutures.forEach((future) -> future.cancel(true));
			CompletableFuture.allOf(allCompletableFutures.stream().toArray(CompletableFuture[]::new))
					.whenComplete((unusedVoid, unusedThrowable) -> {
						// when all futures are done, remove the session to
						// brain mapping
						sessionIdToSessionInfo.remove(sessionId);
					});
		}
	}

	/**
	 * Returns {@link CompletionStage} that will be completed when the given
	 * {@link RequestToClient} is completed by the client.
	 *
	 * @param sessionId
	 * @param completableMessage
	 * @return
	 */
	public CompletionStage<ResponseFromClient> listenForCompletedMessage(@NonNull String sessionId,
			@NonNull RequestToClient completableMessage) {
		template.convertAndSendToUser(sessionId, "/topic/request", completableMessage);
		final SessionInfo sessionInfo = sessionIdToSessionInfo.get(sessionId);
		if (sessionInfo == null) {
			throw new IllegalStateException(
					"Try to register a completion message for a session that doesn't exist. Session id: " + sessionId);
		} else {
			final String id = completableMessage.getId();
			if (id == null || id.isEmpty()) {
				throw new IllegalArgumentException("id must be set");
			}
			final Map<String, CompletableFuture<ResponseFromClient>> completableIdToCompletableFutures = sessionInfo
					.getCompletableIdToCompletableFutures();
			final CompletableFuture<ResponseFromClient> completableFuture = new CompletableFuture<>();
			completableFuture.whenComplete((unusedVoid, throwable) -> {
				// remove this completable future from the map when done
				completableIdToCompletableFutures.remove(id);
			});
			if (completableIdToCompletableFutures.putIfAbsent(id, completableFuture) == null) {
				return completableFuture;
			} else {
				throw new IllegalStateException(
						"There is already a completable future registered in session " + sessionId + " for id " + id);
			}
		}
	}

	private void onApplicationEvent(SessionDisconnectEvent event) {
		log.info("Disconnected. Close status: {}", event.getCloseStatus());
		final String sessionId = event.getSessionId();
		endSession(sessionId);
	}

	@Override
	public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
		AbstractWebPlugin.applicationContext = applicationContext;
	}

	@SneakyThrows
	public void start(String sessionId) {
		if (sessionIdToSessionInfo.get(sessionId) != null) {
			throw new IllegalStateException("Already started");
		}
		Brain brain = sessionIdToSessionInfo.computeIfAbsent(sessionId, (x) -> {
			try {
				return new SessionInfo(Commander.getBrain(brainProperties.getConfiguration().getURL(),
						Collections.singletonMap("sessionId", sessionId)));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}).getBrain();
		brain.runAsync();
		brain.getCompletableFuture().whenComplete((unused1, throwable) -> {
			template.convertAndSendToUser(sessionId, "/topic/shutdown",
					ShutdownOutputMessage.builder().message(
							throwable == null ? "The system has shut down" : "The system has shutdown due to an error")
					.build());
			endSession(sessionId);
		});
	}
}

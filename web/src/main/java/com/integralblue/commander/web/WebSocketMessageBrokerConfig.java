package com.integralblue.commander.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.user.UserDestinationResolver;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketMessageBrokerConfig extends AbstractWebSocketMessageBrokerConfigurer {

	private static final int MAX_MESSAGE_SIZE = 16 * 1024 * 1024;

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		// use the /topic prefix for outgoing WebSocket communication
		config.enableSimpleBroker("/topic");

		// use the /app prefix for others
		config.setApplicationDestinationPrefixes("/app");

		config.setUserDestinationPrefix("/user");
	}

	@Override
	public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
		super.configureWebSocketTransport(registration);
		registration.setMessageSizeLimit(MAX_MESSAGE_SIZE); // 1 MB
	}

	@Bean
	public ServletServerContainerFactoryBean createWebSocketContainer() {
		ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
		container.setMaxTextMessageBufferSize(MAX_MESSAGE_SIZE);
		container.setMaxBinaryMessageBufferSize(MAX_MESSAGE_SIZE);
		return container;
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/stomp").withSockJS();
	}

	@Bean
	public UserDestinationResolver userDestinationResolver() {
		return new SessionIdUserDestinationResolver();
	}

}
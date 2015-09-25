package com.integralblue.commander.web.plugins;

import java.util.concurrent.CompletionStage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.util.Assert;

import com.integralblue.commander.api.AbstractPlugin;
import com.integralblue.commander.web.message.ConsoleOutputMessage;
import com.integralblue.commander.web.message.RequestToClient;
import com.integralblue.commander.web.message.ResponseFromClient;
import com.integralblue.commander.web.service.BrainService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractWebPlugin extends AbstractPlugin {
	public static ApplicationContext applicationContext;
	protected String sessionId;

	@Autowired
	protected SimpMessagingTemplate template;

	@Autowired
	protected BrainService brainService;

	@Override
	public void initialize() throws Exception {
		sessionId = config.getString("sessionId");

		// WebPlugin isn't instantiated by Spring, so injection doesn't occur
		// automatically. So we can use Spring beans, perform injection on this
		// instance.
		Assert.notNull(applicationContext, "applicationContext must be provided");
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(applicationContext.getAutowireCapableBeanFactory());
		bpp.processInjection(this);
	}

	/**
	 * Sends text to the client for display on the console
	 *
	 * @param text
	 */
	protected void println(String text) {
		log.debug(text);
		template.convertAndSendToUser(sessionId, "/topic/console", ConsoleOutputMessage.builder().text(text).build());
	}

	protected CompletionStage<ResponseFromClient> sendRequestToClient(RequestToClient requestToClient) {
		return brainService.listenForCompletedMessage(sessionId, requestToClient);
	}

}

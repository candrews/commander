package com.integralblue.commander.web.controller;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import com.integralblue.commander.web.message.ResponseFromClient;
import com.integralblue.commander.web.service.BrainService;

import lombok.NonNull;

@Controller
public class ResponseController {
	@Autowired
	private BrainService brainService;

	@MessageMapping("/response")
	public void completed(@NonNull @NotNull ResponseFromClient responseFromClient,
			@NonNull SimpMessageHeaderAccessor smha) {
		brainService.completedMessage(smha.getSessionId(), responseFromClient);
	}
}

package com.integralblue.commander.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import com.integralblue.commander.web.service.BrainService;

import lombok.NonNull;

@Controller
public class StartController {
	@Autowired
	private BrainService brainService;

	@MessageMapping("/start")
	public void completed(@NonNull SimpMessageHeaderAccessor smha) {
		brainService.start(smha.getSessionId());
	}
}

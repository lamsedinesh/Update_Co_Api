package com.dinesh.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dinesh.response.CoResponse;
import com.dinesh.service.CoService;

@RestController
public class CoRestController {
	@Autowired
	private CoService service;

	@GetMapping("/process")
	public CoResponse processTriggers() {
		return service.processPendingTriggers();

	}
}

package com.naonsoft.example.controller.rest;

import java.security.Principal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

	@GetMapping(value = "group1")
	public String group1(Principal principal) {
		return "Group 1";
	}

	@GetMapping(value = "group2")
	public String group2() {
		return "Group 2";
	}
}

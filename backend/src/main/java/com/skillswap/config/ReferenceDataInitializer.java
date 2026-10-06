package com.skillswap.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.skillswap.reputation.ReputationService;

/** Ensures reference data the code depends on (the badge catalogue) exists in every environment. */
@Component
@Order(1)
public class ReferenceDataInitializer implements ApplicationRunner {

	private final ReputationService reputationService;

	public ReferenceDataInitializer(ReputationService reputationService) {
		this.reputationService = reputationService;
	}

	@Override
	public void run(ApplicationArguments args) {
		reputationService.ensureBadgeCatalog();
	}
}

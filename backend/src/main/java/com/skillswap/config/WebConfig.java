package com.skillswap.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.skillswap.common.storage.FileStorageService;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final FileStorageService storage;

	public WebConfig(FileStorageService storage) {
		this.storage = storage;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/uploads/**")
				.addResourceLocations(storage.getRoot().toUri().toString() + "/")
				.setCachePeriod(3600);
	}
}

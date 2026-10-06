package com.skillswap;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class SkillswapApplication {

	public static void main(String[] args) {
		String zone = System.getenv().getOrDefault("APP_TIMEZONE", "Asia/Kolkata");
		TimeZone.setDefault(TimeZone.getTimeZone(zone));
		SpringApplication.run(SkillswapApplication.class, args);
	}

}

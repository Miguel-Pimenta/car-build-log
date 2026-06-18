package com.miguelpimenta.buildlog;

import org.springframework.boot.SpringApplication;

public class TestCarBuildLogApplication {

	public static void main(String[] args) {
		SpringApplication.from(CarBuildLogApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

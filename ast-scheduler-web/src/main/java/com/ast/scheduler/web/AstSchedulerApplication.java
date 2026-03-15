package com.ast.scheduler.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.ast.scheduler")
@EnableScheduling
public class AstSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AstSchedulerApplication.class, args);
    }
}
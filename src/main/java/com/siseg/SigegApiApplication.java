package com.siseg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SigegApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(SigegApiApplication.class, args);
    }
}



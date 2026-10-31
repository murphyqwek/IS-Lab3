package com.example.is;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ISApplication {

    public static void main(String[] args) {
        SpringApplication.run(ISApplication.class, args);
    }

}

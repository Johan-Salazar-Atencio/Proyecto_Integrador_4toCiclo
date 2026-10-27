package com.plantia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PlantiaApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlantiaApplication.class, args);
    }
}

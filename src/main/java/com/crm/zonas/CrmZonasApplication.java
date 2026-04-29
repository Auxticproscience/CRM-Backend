package com.crm.zonas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CrmZonasApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrmZonasApplication.class, args);
    }
}


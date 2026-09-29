package com.medilinkpro.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // elargissement periodique des alertes sans reponse (AlerteService)
public class MedilinkproBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedilinkproBackendApplication.class, args);
    }

}

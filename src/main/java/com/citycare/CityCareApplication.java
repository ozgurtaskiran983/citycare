package com.citycare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CityCareApplication - Uygulamanin ana sinifi ve baslangic noktasi.
 *
 * Spring Boot'un otomatik yapilandirmasini (@SpringBootApplication) etkinlestirir.
 * Uygulama bu siniftaki main metodu uzerinden ayaga kalkar.
 */
@SpringBootApplication
public class CityCareApplication {

    public static void main(String[] args) {
        SpringApplication.run(CityCareApplication.class, args);
    }
}

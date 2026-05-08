package com.citycare.config;

import com.citycare.entity.Role;
import com.citycare.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * DataInitializer - Uygulama baslangicinda varsayilan hesaplari olusturur.
 *
 * CommandLineRunner arayuzunu implement eder; Spring context hazir olduktan
 * sonra bir kez calisir.
 * existsByUsername kontrolleri sayesinde her yeniden baslatisinда guvenle
 * calisir (idempotent).
 *
 * Olusturulan varsayilan hesaplar:
 *   admin    / admin123    -> ROLE_ADMIN
 *   vatandas / vatandas123 -> ROLE_USER
 *
 * ONEMLI: Uygulamayi production ortamina almadan once varsayilan sifreleri degistirin!
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;

    @Override
    public void run(String... args) {
        // Varsayilan admin hesabi yoksa olustur
        if (!userService.existsByUsername("admin")) {
            userService.register("admin", "admin123", Role.ADMIN);
            log.info("Varsayilan admin hesabi olusturuldu: admin / admin123");
        }
        // Varsayilan vatandas test hesabi yoksa olustur
        if (!userService.existsByUsername("vatandas")) {
            userService.register("vatandas", "vatandas123", Role.USER);
            log.info("Varsayilan vatandas hesabi olusturuldu: vatandas / vatandas123");
        }
    }
}

package com.citycare.config;

import com.citycare.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * SecurityConfig - CityCare uygulamasi icin Spring Security yapılandirmasi.
 *
 * Kimlik dogrulama:
 *   - UserDetailsServiceImpl ile yapılandırılmıs DaoAuthenticationProvider kullanilir.
 *   - Sifreler guc faktoru 12 olan BCrypt algoritmasi ile hashlenir.
 *
 * Erisim kurallari:
 *   - /login, /register, /css/**, /js/**, /images/**, /error -> herkese acik (giris gerekmez)
 *   - /admin/**  -> Yalnizca ROLE_ADMIN
 *   - Diger tum yollar -> Kimlik dogrulanmis herhangi bir kullanici
 *
 * Giris / Cikis:
 *   - Giris formu /login adresinde, POST /login'de islenir.
 *   - Basarili giris /reports'a yonlendirir.
 *   - Basarisiz giris /login?error=true'ya yonlendirir.
 *   - Cikis POST /logout ile (CSRF korumalı), basarida /login?logout=true'ya yonlendirir.
 *
 * Oturum yonetimi:
 *   - Kullanici basina en fazla 1 eszamanli oturum.
 *   - Suresi dolan oturumlar /login?expired=true'ya yonlendirilir.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;

    /** Guc faktoru 12 ile BCrypt sifre kodlayici. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * UserDetailsService ve PasswordEncoder'i birbirine baglayan kimlik dogrulama saglayicisi.
     * Spring Security tarafindan giris sirasinda kimlik bilgilerini dogrulamak icin kullanilir.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /** Tum erisim kurallariyla ana guvenlik filtre zincirini yapilandirir. */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/css/**", "/js/**",
                                 "/images/**", "/webjars/**", "/error").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/reports", true)
                .failureUrl("/login?error=true")
                .usernameParameter("username")
                .passwordParameter("password")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .clearAuthentication(true)
                .permitAll()
            )
            .sessionManagement(session -> session
                .maximumSessions(1)
                .expiredUrl("/login?expired=true")
            );
        return http.build();
    }
}

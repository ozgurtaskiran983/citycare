package com.citycare.controller;

import com.citycare.entity.Role;
import com.citycare.service.UserService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * AuthController - Kimlik dogrulama ile ilgili sayfa ve islemleri yonetir.
 *
 * Rota ozeti:
 *   GET  /          -> /reports adresine yonlendirir
 *   GET  /login     -> Giris sayfasini gosterir (error/logout/expired sorgu parametreleriyle)
 *   GET  /register  -> Kayit formunu gosterir
 *   POST /register  -> Kayit islemini gerceklestirir (ROLE_USER hesabi olusturur)
 *
 * Asil giris POST istegi (/login), bu controller tarafindan degil
 * Spring Security'nin UsernamePasswordAuthenticationFilter'i tarafindan islenir.
 */
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    // ── ANA SAYFA YONLENDIRME ─────────────────────────────────────────────────

    /** Kok URL'yi ihbar listesine yonlendirir. */
    @GetMapping("/")
    public String home() { return "redirect:/reports"; }

    // ── GIRIS SAYFASI ─────────────────────────────────────────────────────────

    /**
     * Giris sayfasini gosterir.
     * Sorgu parametrelerine gore baglam mesajlari set edilir:
     *   ?error   -> "Gecersiz kullanici adi veya sifre"
     *   ?logout  -> "Basariyla cikis yapildi"
     *   ?expired -> "Oturumunuz sona erdi"
     */
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String expired,
            Model model) {

        if (error   != null) model.addAttribute("errorMsg",
                "Geçersiz kullanıcı adı veya şifre!");
        if (logout  != null) model.addAttribute("logoutMsg",
                "Başarıyla çıkış yapıldı.");
        if (expired != null) model.addAttribute("errorMsg",
                "Oturumunuz sona erdi. Lütfen tekrar giriş yapın.");
        return "auth/login";
    }

    // ── KAYIT ─────────────────────────────────────────────────────────────────

    /** Kayit formunu gosterir. */
    @GetMapping("/register")
    public String registerPage() { return "auth/register"; }

    /**
     * Kayit formunu isler:
     *   1. Sifre eslesmesini dogrular.
     *   2. Kullanici adi benzersizligini kontrol eder.
     *   3. BCrypt sifrelemesini UserService.register()'a devreder.
     *   4. Basarida /login'e, basarisizlikta /register'a yonlendirir.
     *
     * Yeni hesaplar her zaman ROLE_USER alir. ROLE_ADMIN yalnizca DataInitializer araciligiyla olusturulur.
     */
    @PostMapping("/register")
    public String register(
            @RequestParam @NotBlank @Size(min = 3, max = 50) String username,
            @RequestParam @NotBlank @Size(min = 6)           String password,
            @RequestParam @NotBlank                          String confirmPassword,
            RedirectAttributes ra) {

        if (!password.equals(confirmPassword)) {
            ra.addFlashAttribute("errorMsg", "Şifreler eşleşmiyor!");
            return "redirect:/register";
        }
        if (userService.existsByUsername(username)) {
            ra.addFlashAttribute("errorMsg", "Bu kullanıcı adı zaten kullanılıyor!");
            return "redirect:/register";
        }
        try {
            userService.register(username, password, Role.USER);
            ra.addFlashAttribute("successMsg", "Kayıt başarılı! Giriş yapabilirsiniz.");
            return "redirect:/login";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Kayıt sırasında hata: " + e.getMessage());
            return "redirect:/register";
        }
    }
}

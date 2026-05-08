package com.citycare.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * GlobalExceptionHandler - @ControllerAdvice kullanilarak tanimlanan uygulama genelinde hata isleyici.
 *
 * Yaygin istisna turlerini kullanici dostu hata sayfalarına yonlendirir:
 *   RuntimeException        -> 404 bulunamadi sayfasi  (ihbar/kullanici bulunamadi durumlari)
 *   SecurityException       -> 403 erisim reddedildi sayfasi  (yetkisiz islem girisimleri)
 *   IllegalArgumentException -> Flash mesajla /reports adresine yonlendirme
 *
 * Hata sayfasi sablonlari templates/error/ klasoründe yer alir.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Varlik bulunamadi hatalarini isler (servis katmanindan firlatilan RuntimeException).
     * 404 hata sablonunu aciklayici bir mesajla gosterir.
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(RuntimeException ex, Model model) {
        log.warn("Kaynak bulunamadi: {}", ex.getMessage());
        model.addAttribute("errorTitle",   "Kaynak Bulunamadı");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/not-found";
    }

    /**
     * Yetkisiz erisim girisimlerini isler (servis katmanindan firlatilan SecurityException).
     * 403 erisim reddedildi sablonunu gosterir.
     */
    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleForbidden(SecurityException ex, Model model) {
        log.warn("Yetkisiz erisim: {}", ex.getMessage());
        model.addAttribute("errorTitle",   "Erişim Reddedildi");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/forbidden";
    }

    /**
     * Dogrulama hatalarini isler (orn. gecersiz goruntu turu/boyutu).
     * Hata sayfasi gostermek yerine /reports adresine flash hata mesajiyla yonlendirir.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleBadRequest(IllegalArgumentException ex,
                                   org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        log.warn("Gecersiz istek: {}", ex.getMessage());
        ra.addFlashAttribute("errorMsg", ex.getMessage());
        return "redirect:/reports";
    }
}

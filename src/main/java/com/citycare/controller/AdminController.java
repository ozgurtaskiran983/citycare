package com.citycare.controller;

import com.citycare.dto.StatusUpdateDTO;
import com.citycare.entity.*;
import com.citycare.service.ReportService;
import com.citycare.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * AdminController - Tum admin'e ozgu islemleri yonetir.
 *
 * Bu controller'daki tum rotalar @PreAuthorize ile ROLE_ADMIN gerektirir.
 *
 * Temel sorumluluklar:
 *   - Pano istatistik ozeti
 *   - Ihbar yonetimi (durum guncelleme, aciliyet guncelleme, silme)
 *   - Kullanici yonetimi (listeleme, etkinlestirme/engelleme)
 *
 * Rota ozeti:
 *   GET  /admin                          -> Pano (dashboard)
 *   GET  /admin/reports                  -> Filtreli sayfalanmis ihbar listesi
 *   POST /admin/reports/{id}/status      -> Durum guncelleme + opsiyonel geri bildirim
 *   POST /admin/reports/{id}/urgency     -> Aciliyet seviyesi guncelleme (1-5)
 *   POST /admin/reports/{id}/delete      -> Ihrabi sil
 *   GET  /admin/users                    -> Tum vatandas hesaplarini listele
 *   POST /admin/users/{id}/toggle        -> Kullanici hesabini etkinlestir veya engelle
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final ReportService reportService;
    private final UserService   userService;

    // ── PANO ─────────────────────────────────────────────────────────────────

    /**
     * Sistem geneli istatistikleri iceren admin panosunu gosterir.
     * Ihbar durumu sayimlari ve kullanici hesabi toplamlarini hesaplar.
     */
    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("totalReports",  reportService.countAll());
        model.addAttribute("openCount",     reportService.countByStatus(ReportStatus.ACIK));
        model.addAttribute("reviewCount",   reportService.countByStatus(ReportStatus.INCELEMEDE));
        model.addAttribute("solvedCount",   reportService.countByStatus(ReportStatus.COZULDU));
        model.addAttribute("rejectedCount", reportService.countByStatus(ReportStatus.REDDEDILDI));
        model.addAttribute("totalUsers",    userService.countAll());
        model.addAttribute("enabledUsers",  userService.countEnabled());
        return "admin/dashboard";
    }

    // ── IHBAR YONETIMI ────────────────────────────────────────────────────────

    /**
     * Tum ihbarlari opsiyonel anahtar kelime, kategori, durum, aciliyet ve
     * siralama filtreleriyle listeler. Sonuclar sayfa basina 15 kayit olacak sekilde sayfalanir.
     */
    @GetMapping("/reports")
    public String reports(
            @RequestParam(required = false)           String keyword,
            @RequestParam(required = false)           ReportCategory category,
            @RequestParam(required = false)           ReportStatus status,
            @RequestParam(defaultValue = "0")         int urgency,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "15")        int size,
            Model model) {

        Page<Report> reportPage = reportService.searchReports(
                keyword, category, status, urgency, sortBy, page, size);

        model.addAttribute("reports",          reportPage.getContent());
        model.addAttribute("currentPage",      page);
        model.addAttribute("totalPages",       reportPage.getTotalPages());
        model.addAttribute("totalItems",       reportPage.getTotalElements());
        model.addAttribute("keyword",          keyword);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedStatus",   status);
        model.addAttribute("selectedUrgency",  urgency);
        model.addAttribute("sortBy",           sortBy);
        model.addAttribute("categories",       ReportCategory.values());
        model.addAttribute("statuses",         ReportStatus.values());
        model.addAttribute("totalCount",  reportService.countAll());
        model.addAttribute("openCount",   reportService.countByStatus(ReportStatus.ACIK));
        model.addAttribute("solvedCount", reportService.countByStatus(ReportStatus.COZULDU));
        return "admin/reports";
    }

    /**
     * Bir ihbarin durumunu gunceller ve opsiyonel admin notu ile cozum fotografini kaydeder.
     * Kayit sonrasinda ihbar detay sayfasina yonlendirir.
     */
    @PostMapping("/reports/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @ModelAttribute StatusUpdateDTO dto,
                               RedirectAttributes ra) {
        try {
            reportService.updateStatus(id, dto);
            ra.addFlashAttribute("successMsg",
                    "Durum güncellendi: " + dto.getNewStatus().getDisplayName());
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Hata: " + e.getMessage());
        }
        return "redirect:/reports/" + id;
    }

    /**
     * Bir ihbarin aciliyet seviyesini (1-5) gunceller.
     * Admin ihbar listesindeki hizli secim dropdown'i tarafindan cagrilir.
     * Guncelleme sonrasinda admin bulundugu sayfaya geri doner.
     */
    @PostMapping("/reports/{id}/urgency")
    public String updateUrgency(@PathVariable Long id,
                                @RequestParam int urgency,
                                @RequestParam(defaultValue = "0") int page,
                                RedirectAttributes ra) {
        try {
            reportService.updateUrgency(id, urgency);
            ra.addFlashAttribute("successMsg", "Aciliyet güncellendi.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/reports?page=" + page;
    }

    /**
     * Bir ihrabi kalici olarak siler. Adminler herhangi bir ihrabi silebilir;
     * vatandaslar yalnizca kendi ihbarlarini silebilir.
     */
    @PostMapping("/reports/{id}/delete")
    public String deleteReport(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            reportService.deleteReport(id, auth.getName(), true);
            ra.addFlashAttribute("successMsg", "İhbar silindi.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/reports";
    }

    // ── KULLANICI YONETIMI ────────────────────────────────────────────────────

    /**
     * Tum vatandas hesaplarini (ROLE_USER) listeleyen kullanici yonetim sayfasini gosterir.
     * Admin hesaplari bu listeden cikarilir.
     */
    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users",       userService.findAllCitizens());
        model.addAttribute("totalCount",  reportService.countAll());
        model.addAttribute("openCount",   reportService.countByStatus(ReportStatus.ACIK));
        model.addAttribute("solvedCount", reportService.countByStatus(ReportStatus.COZULDU));
        return "admin/users";
    }

    /**
     * Bir kullanicinin etkin/engelli durumunu degistirir.
     *
     * HATA DUZELTME NOTU: wasEnabled degiskeni setEnabled() ONCESINDE alinmalidir.
     * Cunku servis cagrisi Hibernate birinci seviye onbellegi uzerinden varlik nesnesini
     * guncelleyebilir ve bu durumda user.isEnabled() yeni degeri dondurur.
     * wasEnabled onceden alindiginda flash mesaji her zaman dogru olur:
     *   wasEnabled=true  -> kullanici ENGELLENIYOR  -> mesaj: "Engellendi"
     *   wasEnabled=false -> kullanici AKTIF EDILIYOR -> mesaj: "Aktif edildi"
     */
    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id, RedirectAttributes ra) {
        try {
            var user = userService.findById(id);
            boolean wasEnabled = user.isEnabled();          // toggle oncesinde durumu yakala
            userService.setEnabled(id, !wasEnabled);
            ra.addFlashAttribute("successMsg",
                    wasEnabled ? "Kullanıcı engellendi." : "Kullanıcı aktif edildi.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}

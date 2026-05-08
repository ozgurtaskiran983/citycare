package com.citycare.controller;

import com.citycare.dto.ReportDTO;
import com.citycare.entity.*;
import com.citycare.service.ReportService;
import com.citycare.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * ReportController - Vatandas odakli ihbar islemlerini yonetir.
 *
 * Bu controller'da uygulanan erisim kurallari:
 *   - Admin, olustur ve duzenle endpoint'lerinden ENGELLENIR (yonlendirilir).
 *   - Duzenleme yalnizca ihbarin sahibine izin verilir.
 *   - Silme, sahibe veya admin'e izin verilir (admin silme AdminController'dan gecer).
 *
 * Rota ozeti:
 *   GET  /reports                -> Arama filtreleriyle sayfalanmis liste
 *   GET  /reports/my             -> Gecerli kullanicinin kendi ihbarlari
 *   GET  /reports/{id}           -> Ihbar detay sayfasi
 *   GET  /reports/create         -> Yeni ihbar formu (yalnizca vatandaslar)
 *   POST /reports/create         -> Yeni ihbari kaydet
 *   GET  /reports/{id}/edit      -> Duzenleme formu (yalnizca sahip)
 *   POST /reports/{id}/edit      -> Degisiklikleri kaydet
 *   POST /reports/{id}/delete    -> Ihrabi sil (sahip veya admin)
 */
@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;

    // ── LISTE VE ARAMA ────────────────────────────────────────────────────────

    /**
     * Tum kullanicilar (vatandas veya admin) icin sayfalanmis ihbar listesini gosterir.
     * Anahtar kelime, kategori, durum, aciliyet ve siralama ile filtrelemeyi destekler.
     * Kenar cubugu istatistiklerini (toplam, acik, cozuldu sayilari) de model'e ekler.
     */
    @GetMapping
    public String listReports(
            @RequestParam(required = false)           String keyword,
            @RequestParam(required = false)           ReportCategory category,
            @RequestParam(required = false)           ReportStatus status,
            @RequestParam(defaultValue = "0")         int urgency,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "9")         int size,
            Authentication auth, Model model) {

        boolean isAdmin = isAdmin(auth);
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
        model.addAttribute("isAdmin",          isAdmin);
        model.addAttribute("totalCount",  reportService.countAll());
        model.addAttribute("openCount",   reportService.countByStatus(ReportStatus.ACIK));
        model.addAttribute("solvedCount", reportService.countByStatus(ReportStatus.COZULDU));
        return "reports/list";
    }

    // ── IHBARLARIM ────────────────────────────────────────────────────────────

    /**
     * Yalnizca kimlik dogrulanmis vatandasin kendi ihbarlarini opsiyonel filtrelerle gosterir.
     * Admin bu sayfaya erisemez; admin ihbar paneline yonlendirilir.
     */
    @GetMapping("/my")
    public String myReports(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) ReportCategory category,
                            Authentication auth, Model model) {
        if (isAdmin(auth)) return "redirect:/admin/reports";

        model.addAttribute("reports",
                reportService.searchMyReports(auth.getName(), keyword, category));
        model.addAttribute("keyword",          keyword);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("categories",       ReportCategory.values());
        return "reports/my-reports";
    }

    // ── DETAY ─────────────────────────────────────────────────────────────────

    /**
     * Tek bir ihbar icin tam detay sayfasini gosterir.
     * Hangi aksiyon butonlarinin goruntulenegini kontrol etmek icin
     * isAdmin ve isOwner bayraklarini model'e aktarir.
     * Admin geri bildirim formu ve durum guncelleme bolumu yalnizca adminlere gorunur.
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Authentication auth, Model model) {
        Report report = reportService.findById(id);
        boolean isAdmin = isAdmin(auth);
        boolean isOwner = report.getUser().getUsername().equals(auth.getName());

        model.addAttribute("report",   report);
        model.addAttribute("isAdmin",  isAdmin);
        model.addAttribute("isOwner",  isOwner);
        model.addAttribute("statuses", ReportStatus.values());
        return "reports/detail";
    }

    // ── OLUSTURMA (Yalnizca vatandaslar) ──────────────────────────────────────

    /**
     * Yeni ihbar formunu gosterir. Admin erisirse admin paneline yonlendirilir.
     */
    @GetMapping("/create")
    public String createForm(Authentication auth, Model model) {
        if (isAdmin(auth)) return "redirect:/admin/reports?error=admin-cannot-create";
        model.addAttribute("reportDTO",  new ReportDTO());
        model.addAttribute("categories", ReportCategory.values());
        return "reports/create";
    }

    /**
     * Yeni ihbar formunun gonderimini isler.
     * Dogrulama basarisiz olursa form hata mesajlariyla yeniden gosterilir.
     * Basarida, onay flash mesajiyla birlikte "Ihbarlarim" sayfasina yonlendirilir.
     */
    @PostMapping("/create")
    public String createReport(@Valid @ModelAttribute("reportDTO") ReportDTO dto,
                               BindingResult br, Authentication auth,
                               RedirectAttributes ra, Model model) {
        if (isAdmin(auth)) return "redirect:/admin/reports";
        if (br.hasErrors()) {
            model.addAttribute("categories", ReportCategory.values());
            return "reports/create";
        }
        try {
            Report saved = reportService.createReport(dto, auth.getName());
            ra.addFlashAttribute("successMsg",
                    "İhbarınız kaydedildi! (ID: " + saved.getId() + ")");
            return "redirect:/reports/my";
        } catch (Exception e) {
            model.addAttribute("categories", ReportCategory.values());
            model.addAttribute("errorMsg", "Hata: " + e.getMessage());
            return "reports/create";
        }
    }

    // ── DUZENLEME (Yalnizca sahip) ────────────────────────────────────────────

    /**
     * Mevcut ihbar verileriyle doldurulmus duzenleme formunu gosterir.
     * Admin bu sayfaya erisirse detay sayfasina yonlendirilir (icerik duzenleyemez).
     * Sahip olmayan kullanicilar yetkisiz hata ile yonlendirilir.
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Authentication auth, Model model) {
        if (isAdmin(auth)) return "redirect:/reports/" + id;

        Report report = reportService.findById(id);
        if (!report.getUser().getUsername().equals(auth.getName())) {
            return "redirect:/reports?error=unauthorized";
        }
        ReportDTO dto = new ReportDTO();
        dto.setId(report.getId());
        dto.setTitle(report.getTitle());
        dto.setDescription(report.getDescription());
        dto.setCategory(report.getCategory());
        dto.setUrgency(report.getUrgency());
        dto.setLocation(report.getLocation());

        model.addAttribute("reportDTO",  dto);
        model.addAttribute("report",     report);
        model.addAttribute("categories", ReportCategory.values());
        return "reports/edit";
    }

    /**
     * Duzenlenmis ihbar icerigini kaydeder. Dogrulama hatasinda yeniden gosterir.
     * Yeni dosya yuklenmezse mevcut goruntu korunur.
     */
    @PostMapping("/{id}/edit")
    public String updateReport(@PathVariable Long id,
                               @Valid @ModelAttribute("reportDTO") ReportDTO dto,
                               BindingResult br, Authentication auth,
                               RedirectAttributes ra, Model model) {
        if (isAdmin(auth)) return "redirect:/reports/" + id;
        if (br.hasErrors()) {
            model.addAttribute("report",     reportService.findById(id));
            model.addAttribute("categories", ReportCategory.values());
            return "reports/edit";
        }
        try {
            reportService.updateReport(id, dto, auth.getName());
            ra.addFlashAttribute("successMsg", "İhbar başarıyla güncellendi.");
            return "redirect:/reports/" + id;
        } catch (Exception e) {
            model.addAttribute("errorMsg", "Hata: " + e.getMessage());
            model.addAttribute("categories", ReportCategory.values());
            return "reports/edit";
        }
    }

    // ── SILME ─────────────────────────────────────────────────────────────────

    /**
     * Bir ihrabi siler. Vatandaslar yalnizca kendi ihbarlarini; adminler herhangi bir ihrabi silebilir.
     * Silme sonrasinda adminler admin paneline, vatandaslar kendi listelerine yonlendirilir.
     */
    @PostMapping("/{id}/delete")
    public String deleteReport(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        boolean admin = isAdmin(auth);
        try {
            reportService.deleteReport(id, auth.getName(), admin);
            ra.addFlashAttribute("successMsg", "İhbar silindi.");
        } catch (SecurityException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return admin ? "redirect:/admin/reports" : "redirect:/reports/my";
    }

    // ── OZEL YARDIMCI METOTLAR ────────────────────────────────────────────────

    /** Kimlik dogrulanmis kullanicinin ROLE_ADMIN rolune sahip olup olmadigini dondurur. */
    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities()
                   .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}

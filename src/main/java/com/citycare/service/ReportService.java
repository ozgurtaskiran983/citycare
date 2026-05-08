package com.citycare.service;

import com.citycare.dto.ReportDTO;
import com.citycare.dto.StatusUpdateDTO;
import com.citycare.entity.*;
import com.citycare.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ReportService - Ihbar (Report) islemleri icin temel is mantigi katmani.
 *
 * Sorumluluklar:
 *   - Yeni ihbar olusturma (yalnizca vatandaslar; adminler engellenir)
 *   - Sayfalama destekli ihbar arama ve filtreleme
 *   - Admin geri bildirimiyle (not + fotograf) ihbar durumu guncelleme
 *   - Ihbar aciliyet seviyesini bagimsiz olarak guncelleme
 *   - Ihbar icerigini guncelleme (yalnizca vatandas)
 *   - Ihbar silme (sahip veya admin)
 *   - Yuklenen gorsel dosyalarini dogrulama (MIME turu + boyut kontrolu)
 *
 * Tum yazma islemleri @Transactional ile; okuma islemleri performans icin
 * @Transactional(readOnly=true) ile isaretlenmistir.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserService userService;

    // ── OLUSTURMA ─────────────────────────────────────────────────────────────

    /**
     * Bir vatandas kullanici icin yeni ihbar olusturur.
     * Cagiran kullanici ADMIN rolundeyse SecurityException firlatir (adminler ihbar olusturamaz).
     * Fotograf yuklenmisse dogrulama yapilarak LONGBLOB olarak kaydedilir.
     *
     * @param dto      Baslik, aciklama, kategori, aciliyet, konum iceren form verisi.
     * @param username Spring Security'den alinan kimlik dogrulanmis kullanici adi.
     * @return Kaydedilen Report varligi.
     */
    @Transactional
    public Report createReport(ReportDTO dto, String username) throws IOException {
        User user = userService.findByUsername(username);

        if (user.getRole() == Role.ADMIN) {
            throw new SecurityException("Admin kullanıcıları ihbar oluşturamaz.");
        }

        Report report = Report.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .category(dto.getCategory())
                .urgency(dto.getUrgency())
                .location(dto.getLocation())
                .status(ReportStatus.ACIK)
                .user(user)
                .build();

        MultipartFile file = dto.getImageFile();
        if (file != null && !file.isEmpty()) {
            validateImage(file);
            report.setImageData(file.getBytes());
            report.setImageContentType(file.getContentType());
        }

        return reportRepository.save(report);
    }

    // ── OKUMA ─────────────────────────────────────────────────────────────────

    /**
     * Opsiyonel filtrelerle ihbar arama sorgusu calistirir.
     * keyword (baslik LIKE), category, status, urgency (tam esleme) ve sortBy desteklenir.
     * Sayfalanmis Page sonucu doner.
     */
    @Transactional(readOnly = true)
    public Page<Report> searchReports(String keyword, ReportCategory category,
                                      ReportStatus status, int urgency,
                                      String sortBy, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return reportRepository.searchReports(keyword, category, status, urgency,
                sortBy != null ? sortBy : "createdAt", pageable);
    }

    /**
     * Ihrabi ID ile bulur; bulunamazsa RuntimeException firlatir (404 sayfasina yonlendirilir).
     */
    @Transactional(readOnly = true)
    public Report findById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("İhbar bulunamadı: ID=" + id));
    }

    /**
     * Verilen kullanicinin tum ihbarlarini en yeniden en eskiye sirali olarak dondurur.
     */
    @Transactional(readOnly = true)
    public List<Report> findMyReports(String username) {
        User user = userService.findByUsername(username);
        return reportRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    /**
     * Belirli bir kullanicinin ihbarlarini anahtar kelime ve kategori ile arar.
     */
    @Transactional(readOnly = true)
    public List<Report> searchMyReports(String username, String keyword, ReportCategory category) {
        User user = userService.findByUsername(username);
        return reportRepository.searchByUser(user.getId(), keyword, category);
    }

    // ── DURUM GUNCELLEME (Admin geri bildirimiyle) ────────────────────────────

    /**
     * Ihbarin durumunu gunceller ve opsiyonel admin geri bildirimi kaydeder.
     * Admin notu yalnizca bos degilse set edilir.
     * Admin fotografi dogrulanir ve LONGBLOB olarak kaydedilir.
     * feedbackAt zaman damgasi her cagrimda guncellenir.
     *
     * @param id  Guncellenecek ihbarin birincil anahtari.
     * @param dto newStatus, opsiyonel adminNote ve opsiyonel adminImage icerir.
     */
    @Transactional
    public Report updateStatus(Long id, StatusUpdateDTO dto) throws IOException {
        Report report = findById(id);
        report.setStatus(dto.getNewStatus());
        report.setFeedbackAt(LocalDateTime.now());

        if (dto.getAdminNote() != null && !dto.getAdminNote().isBlank()) {
            report.setAdminNote(dto.getAdminNote());
        }

        MultipartFile img = dto.getAdminImage();
        if (img != null && !img.isEmpty()) {
            validateImage(img);
            report.setAdminImageData(img.getBytes());
            report.setAdminImageContentType(img.getContentType());
        }

        return reportRepository.save(report);
    }

    // ── ACILIYET GUNCELLEME ───────────────────────────────────────────────────

    /**
     * Bir ihbarin aciliyet seviyesini (1-5) gunceller.
     * Admin tarafindan ihbar listesinden veya detay sayfasindan dogrudan cagrilir.
     * Gecersiz deger girilirse IllegalArgumentException firlatilir.
     *
     * @param id      Guncellenecek ihbarin birincil anahtari.
     * @param urgency 1 (en dusuk/yesil) ile 5 (en yuksek/kirmizi) arasinda tam sayi.
     */
    @Transactional
    public Report updateUrgency(Long id, int urgency) {
        if (urgency < 1 || urgency > 5) {
            throw new IllegalArgumentException("Aciliyet seviyesi 1-5 arasında olmalıdır.");
        }
        Report report = findById(id);
        report.setUrgency(urgency);
        return reportRepository.save(report);
    }

    // ── ICERIK GUNCELLEME (Yalnizca vatandas) ────────────────────────────────

    /**
     * Vatandasin kendi ihbarinin icerigini (baslik, aciklama, kategori, aciliyet,
     * konum ve opsiyonel fotograf) guncellenmesine izin verir.
     * Cagiran kullanici ihbarin sahibi degilse SecurityException firlatir.
     */
    @Transactional
    public Report updateReport(Long id, ReportDTO dto, String username) throws IOException {
        Report report = findById(id);
        User user = userService.findByUsername(username);

        if (!report.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Bu ihbarı düzenleme yetkiniz yok.");
        }

        report.setTitle(dto.getTitle());
        report.setDescription(dto.getDescription());
        report.setCategory(dto.getCategory());
        report.setUrgency(dto.getUrgency());
        report.setLocation(dto.getLocation());

        MultipartFile file = dto.getImageFile();
        if (file != null && !file.isEmpty()) {
            validateImage(file);
            report.setImageData(file.getBytes());
            report.setImageContentType(file.getContentType());
        }

        return reportRepository.save(report);
    }

    // ── SILME ─────────────────────────────────────────────────────────────────

    /**
     * Bir ihrabi siler.
     * Adminler herhangi bir ihrabi silebilir; vatandaslar yalnizca kendi ihbarlarini silebilir.
     * Admin olmayan bir kullanici baskasinin ihbarini silmeye calisirsa SecurityException firlatilir.
     */
    @Transactional
    public void deleteReport(Long id, String username, boolean isAdmin) {
        Report report = findById(id);
        User user = userService.findByUsername(username);

        if (!isAdmin && !report.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Bu ihbarı silme yetkiniz yok.");
        }
        reportRepository.delete(report);
    }

    // ── ISTATISTIKLER ─────────────────────────────────────────────────────────

    /** Belirtilen durumdaki ihbarlarin sayisini dondurur. */
    @Transactional(readOnly = true)
    public long countByStatus(ReportStatus status) {
        return reportRepository.countByStatus(status);
    }

    /** Sistemdeki toplam ihbar sayisini dondurur. */
    @Transactional(readOnly = true)
    public long countAll() { return reportRepository.count(); }

    // ── OZEL YARDIMCI METOTLAR ────────────────────────────────────────────────

    /**
     * Yuklenen dosyanin bir gorsel (MIME turu image/* ile baslayan) olup olmadigini
     * ve 5 MB boyut sinirini asmayip asmadигini dogrular.
     */
    private void validateImage(MultipartFile file) {
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/"))
            throw new IllegalArgumentException("Sadece resim dosyaları yüklenebilir.");
        if (file.getSize() > 5 * 1024 * 1024)
            throw new IllegalArgumentException("Resim boyutu 5 MB sınırını aşıyor.");
    }
}

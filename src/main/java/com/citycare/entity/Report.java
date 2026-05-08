package com.citycare.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Report (Ihbar) - Vatandas sikayetini temsil eden ana JPA varlik sinifi.
 *
 * Alan ozeti:
 *   title, description, category : Ihbarin temel icerik alanlari.
 *   urgency      : 1-5 arasi aciliyet seviyesi (1=yesil/dusuk, 5=kirmizi/kritik).
 *   location     : Vatandas tarafindan girilen serbest metin adres veya GPS koordinati (opsiyonel).
 *   imageData    : Vatandas tarafindan yuklenen fotograf, MySQL LONGBLOB olarak saklanir.
 *   status       : Ihbarin yasam dongusu durumu (ACIK -> INCELEMEDE -> COZULDU | REDDEDILDI).
 *   adminNote    : Admin tarafindan durum guncellemesi sirasinda yazilan aciklama metni.
 *   adminImageData : Admin tarafindan yuklenen cozum fotografi, MySQL LONGBLOB olarak saklanir.
 *   feedbackAt   : Son admin geri bildiriminin zaman damgasi.
 *   user         : Ihrabi olusturan vatandasa ait yabanci anahtar (FK).
 *
 * @Transient metotlar (veritabanina kaydedilmez):
 *   getBase64Image()  -> Vatandas fotografini Base64 formatina donusturur, HTML icinde kullanilir.
 *   getUrgencyColor() -> Aciliyet balonlarini renklendirmek icin CSS hex rengi dondurur.
 */
@Entity
@Table(name = "reports")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Başlık boş olamaz")
    @Size(min = 5, max = 150)
    @Column(nullable = false, length = 150)
    private String title;

    @NotBlank(message = "Açıklama boş olamaz")
    @Size(min = 10, max = 2000)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Kategori seçilmelidir")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportCategory category;

    /** Vatandas fotografi - MySQL LONGBLOB olarak saklanir. */
    @Lob
    @Column(name = "image_data", columnDefinition = "LONGBLOB")
    private byte[] imageData;

    @Column(name = "image_content_type", length = 50)
    private String imageContentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.ACIK;

    /** Aciliyet seviyesi: 1 (yesil/en dusuk) ile 5 (kirmizi/kritik) arasinda. Varsayilan = 3 (orta). */
    @Min(1) @Max(5)
    @Column(nullable = false)
    @Builder.Default
    private int urgency = 3;

    /** Vatandas tarafindan girilen serbest metin adres veya koordinat bilgisi. */
    @Column(name = "location", length = 300)
    private String location;

    /** Admin tarafindan durum guncellemesi sirasinda birakilan aciklama notu. */
    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    /** Admin cozum fotografi - MySQL LONGBLOB olarak saklanir. */
    @Lob
    @Column(name = "admin_image_data", columnDefinition = "LONGBLOB")
    private byte[] adminImageData;

    @Column(name = "admin_image_content_type", length = 50)
    private String adminImageContentType;

    /** Her admin geri bildirimi gonderildiginde guncellenen zaman damgasi. */
    @Column(name = "feedback_at")
    private LocalDateTime feedbackAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ── YARDIMCI @TRANSIENT METOTLAR ─────────────────────────────────────────

    /** Vatandas fotografini Base64 kodlu String olarak dondurur. Fotograf yoksa null doner. */
    @Transient
    public String getBase64Image() {
        if (imageData == null || imageData.length == 0) return null;
        return Base64.getEncoder().encodeToString(imageData);
    }

    /** Vatandas fotografinin mevcut olup olmadigini kontrol eder. */
    @Transient
    public boolean hasImage() {
        return imageData != null && imageData.length > 0;
    }

    /** Admin cozum fotografinin mevcut olup olmadigini kontrol eder. */
    @Transient
    public boolean hasAdminImage() {
        return adminImageData != null && adminImageData.length > 0;
    }

    /**
     * Aciliyet seviyesini CSS hex renk koduna donusturur.
     * Aciliyet balonu gorsellestirmesi icin kullanilir.
     * 1=yesil, 2=acik yesil, 3=sari, 4=turuncu, 5=kirmizi.
     */
    @Transient
    public String getUrgencyColor() {
        return switch (urgency) {
            case 1 -> "#28a745";
            case 2 -> "#7cb83b";
            case 3 -> "#ffc107";
            case 4 -> "#fd7e14";
            case 5 -> "#dc3545";
            default -> "#6c757d";
        };
    }
}

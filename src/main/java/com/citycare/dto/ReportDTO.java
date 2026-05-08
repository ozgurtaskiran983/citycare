package com.citycare.dto;

import com.citycare.entity.ReportCategory;
import com.citycare.entity.ReportStatus;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * ReportDTO - Ihbar olusturma ve duzenleme formlari icin Veri Transfer Nesnesi.
 *
 * POST /reports/create ve POST /reports/{id}/edit isteklerinde kullanilir.
 * Thymeleaf tarafinda th:object ile forma baglanir.
 *
 * imageFile alani, kullanici tarafindan yuklenen MultipartFile'i tasir.
 * ReportService.createReport() veya updateReport() icinde byte[] dizisine
 * donusturulerek Report varligina kaydedilir.
 *
 * Alanlar uzerindeki dogrulama anotasyonlari (@NotBlank, @Size vb.)
 * controller katmanindaki @Valid ile tetiklenir.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReportDTO {

    /** Birincil anahtar - duzenleme islemlerinde dolu gelir, olusturmada null olur. */
    private Long id;

    @NotBlank(message = "Başlık boş olamaz")
    @Size(min = 5, max = 150, message = "Başlık 5-150 karakter arasında olmalıdır")
    private String title;

    @NotBlank(message = "Açıklama boş olamaz")
    @Size(min = 10, max = 2000, message = "Açıklama 10-2000 karakter arasında olmalıdır")
    private String description;

    @NotNull(message = "Kategori seçilmelidir")
    private ReportCategory category;

    /** Aciliyet seviyesi: 1 (en dusuk/yesil) ile 5 (kritik/kirmizi). Varsayilan 3. */
    @Min(1) @Max(5)
    @Builder.Default
    private int urgency = 3;

    /** Opsiyonel serbest metin adres veya GPS koordinati. */
    @Size(max = 300)
    private String location;

    /** Vatandas tarafindan yuklenen opsiyonel fotograf. ReportService icinde LONGBLOB'a donusturulur. */
    private MultipartFile imageFile;

    /** Form baglamasi icin mevcut; dogrudan form dogrulama islemlerinde kullanilmaz. */
    private ReportStatus status;
}

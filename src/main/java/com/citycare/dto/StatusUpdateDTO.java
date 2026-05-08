package com.citycare.dto;

import com.citycare.entity.ReportStatus;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * StatusUpdateDTO - Admin durum guncelleme formu icin Veri Transfer Nesnesi.
 *
 * POST /admin/reports/{id}/status istegiyle ihbar detay sayfasindan gonderilir.
 * newStatus alani zorunludur; diger alanlar opsiyoneldir:
 *   adminNote  : Yapilan islemi ya da bilgi notunu aciklayan serbest metin.
 *   adminImage : Cozum fotografi, LONGBLOB olarak ihbar kaydinin yaninda saklanir.
 *
 * feedbackAt zaman damgasi ReportService.updateStatus() metodu icinde otomatik olarak set edilir.
 * Fotograf GET /images/feedback/{id} endpoint'i araciligiyla ImageController tarafindan sunulur.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StatusUpdateDTO {

    /** Ihraba uygulanacak yeni yasam dongusu durumu. Zorunlu alandir. */
    private ReportStatus newStatus;

    /** Admin tarafindan vatandaslara gosterilecek opsiyonel aciklama metni. */
    private String adminNote;

    /**
     * Admin tarafindan yuklenen opsiyonel cozum fotografi.
     * ReportService icinde dogrulanir (MIME turu image/* olmali, maksimum 5 MB).
     * ImageController uzerinden GET /images/feedback/{id} ile sunulur.
     */
    private MultipartFile adminImage;
}

package com.citycare.controller;

import com.citycare.entity.Report;
import com.citycare.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * ImageController - Veritabaninda BLOB olarak saklanan goruntuleri HTTP yaniti olarak sunar.
 *
 * Iki endpoint tanimlanmistir:
 *   GET /images/report/{id}   -> Vatandas fotografini sunar (Report.imageData).
 *   GET /images/feedback/{id} -> Admin cozum fotografini sunar (Report.adminImageData).
 *
 * Her iki endpoint de:
 *   - Istenen goruntu alani null/bos ise 404 doner.
 *   - Saklanan MIME turunu Content-Type olarak set eder (yedek: image/jpeg).
 *   - Tekrarlanan veritabani okumalarini azaltmak icin Cache-Control: max-age=300 ekler.
 *
 * Thymeleaf kullanimlari:
 *   <img th:src="@{/images/report/{id}(id=${report.id})}"/>
 *   <img th:src="@{/images/feedback/{id}(id=${report.id})}"/>
 */
@Controller
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

    private final ReportService reportService;

    /**
     * Bir ihraba ait vatandas fotografini sunar.
     * Ihrarin fotografu yoksa 404 doner.
     */
    @GetMapping("/report/{id}")
    public ResponseEntity<byte[]> getReportImage(@PathVariable Long id) {
        Report report = reportService.findById(id);
        if (!report.hasImage()) return ResponseEntity.notFound().build();
        return buildResponse(report.getImageData(), report.getImageContentType());
    }

    /**
     * Bir ihraba ait admin cozum fotografini sunar.
     * Bu ihbar icin admin fotografu yuklenmemisse 404 doner.
     */
    @GetMapping("/feedback/{id}")
    public ResponseEntity<byte[]> getFeedbackImage(@PathVariable Long id) {
        Report report = reportService.findById(id);
        if (!report.hasAdminImage()) return ResponseEntity.notFound().build();
        return buildResponse(report.getAdminImageData(), report.getAdminImageContentType());
    }

    /**
     * Dogru Content-Type basligiyla ve 5 dakikalik onbellekleme ile ResponseEntity olusturur.
     * Saklanan icerik turu null ise varsayilan olarak image/jpeg kullanilir.
     */
    private ResponseEntity<byte[]> buildResponse(byte[] data, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType != null
                ? MediaType.parseMediaType(contentType) : MediaType.IMAGE_JPEG);
        headers.setContentLength(data.length);
        headers.setCacheControl("max-age=300");
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
}

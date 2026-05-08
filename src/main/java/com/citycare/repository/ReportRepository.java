package com.citycare.repository;

import com.citycare.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * ReportRepository - Report varliклari icin Spring Data JPA deposu.
 *
 * JpaRepository'den miras alinan temel CRUD ve sayfalama islemlerine ek olarak
 * ozel JPQL sorgu metotlari tanimlanmistir.
 *
 * Ozel sorgu metotlari:
 *   searchReports : Anahtar kelime, kategori, durum, aciliyet ve siralama secenekleriyle
 *                   admin/vatandas arama sorgusu. Sayfalama (Page) doner.
 *   searchByUser  : Ayni filtreler tek bir kullanicinin ihrarlariyla sinirlandirilmis haliyle.
 *   countByStatus : Admin paneli istatistik kutucuklari icin durum bazli sayim.
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * Belirli bir kullaniciya ait tum ihbarlari en yeniden en eskiye siralar.
     * "Ihbarlarim" sayfasinda kullanilir.
     */
    List<Report> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Opsiyonel filtrelerle dinamik arama sorgusu.
     * keyword, category, status icin null/bos deger ve urgency icin 0 degeri
     * "filtre uygulanmasin" anlamina gelir.
     * sortBy parametresi ORDER BY ifadesini createdAt DESC veya urgency DESC arasinda degistirir.
     *
     * @param keyword  Baslikta buyuk/kucuk harf duyarsiz LIKE eslesme.
     * @param category Tam enum eslesme veya tumu icin null.
     * @param status   Tam enum eslesme veya tumu icin null.
     * @param urgency  Tam int eslesme (1-5) veya tumu icin 0.
     * @param sortBy   Oncelik sirasi icin "urgency", tarih sirasi icin herhangi baska deger.
     */
    @Query("""
        SELECT r FROM Report r
        WHERE (:keyword IS NULL OR :keyword = '' OR
               LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:category IS NULL OR r.category = :category)
          AND (:status   IS NULL OR r.status   = :status)
          AND (:urgency  = 0     OR r.urgency  = :urgency)
        ORDER BY
          CASE WHEN :sortBy = 'urgency' THEN r.urgency END DESC,
          r.createdAt DESC
    """)
    Page<Report> searchReports(
            @Param("keyword")  String keyword,
            @Param("category") ReportCategory category,
            @Param("status")   ReportStatus status,
            @Param("urgency")  int urgency,
            @Param("sortBy")   String sortBy,
            Pageable pageable
    );

    /**
     * Belirli bir kullanicinin ihbarlarini anahtar kelime ve kategori ile arar.
     * Vatandas "Ihbarlarim" sayfasindaki filtre islevinde kullanilir.
     */
    @Query("""
        SELECT r FROM Report r
        WHERE r.user.id = :userId
          AND (:keyword IS NULL OR :keyword = '' OR
               LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:category IS NULL OR r.category = :category)
        ORDER BY r.createdAt DESC
    """)
    List<Report> searchByUser(
            @Param("userId")   Long userId,
            @Param("keyword")  String keyword,
            @Param("category") ReportCategory category
    );

    /** Belirtilen durumdaki ihbar sayisini dondurur. Admin paneli istatistikleri icin kullanilir. */
    long countByStatus(ReportStatus status);

    /** Belirtilen kategorideki ihbar sayisini dondurur. */
    long countByCategory(ReportCategory category);
}

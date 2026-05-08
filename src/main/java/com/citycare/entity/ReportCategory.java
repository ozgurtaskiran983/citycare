package com.citycare.entity;

/**
 * ReportCategory - Ihbar kategorilerini tanimlayan enum.
 *
 * Her kategori, arayuzde gosterilecek displayName alanini tasir.
 * Yeni kategori eklemek icin buraya bir enum sabiti ve displayName degeri eklemek yeterlidir.
 *
 * Mevcut kategoriler:
 *   YOL        -> Yol ve kaldirim bozukluklari
 *   PARK       -> Park ve bahce sorunlari
 *   CEVRE      -> Cevre kirliligi ve temizlik sorunlari
 *   AYDINLATMA -> Sokak lambasi ve aydinlatma arizalari
 *   SU         -> Su baskini, boru patlamasi, kanalizasyon sorunlari
 *   ULASIM     -> Toplu tasima ve trafik sorunlari
 *   DIGER      -> Yukaridaki kategorilere girmeyen diger sorunlar
 */
public enum ReportCategory {
    YOL("Yol & Kaldirim"),
    PARK("Park & Bahce"),
    CEVRE("Cevre & Temizlik"),
    AYDINLATMA("Aydinlatma"),
    SU("Su & Kanalizasyon"),
    ULASIM("Ulasim"),
    DIGER("Diger");

    private final String displayName;

    ReportCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

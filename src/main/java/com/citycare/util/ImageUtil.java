package com.citycare.util;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Base64;
import java.util.Set;

/**
 * ImageUtil - Goruntu islemleri icin yardimci sinif (utility class).
 *
 * Bu sinif iki farkli goruntu gosterim stratejisini destekler:
 *
 * Strateji 1: Base64 Inline (Thymeleaf)
 *   Gorsel dogrudan HTML icine gomulur:
 *   <img th:src="'data:image/jpeg;base64,' + ${report.base64Image}"/>
 *   Avantaj  : Ek endpoint gerekmez.
 *   Dezavantaj: Buyuk listelerde HTML boyutu siser, tarayici onbelleklemesi yapilamaz.
 *
 * Strateji 2: ImageController Endpoint (Onerilir)
 *   Gorsel ayri bir HTTP istegi ile sunulur:
 *   <img th:src="@{/images/report/{id}(id=${report.id})}"/>
 *   Avantaj: Tarayici onbellekleme destegi, lazy loading, temiz HTML ciktisi.
 *   Bu projede ImageController stratejisi kullanilmaktadir.
 */
public final class ImageUtil {

    /** Izin verilen MIME turleri listesi. */
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );

    private ImageUtil() { /* utility sinifi - ornekleme yapilamaz */ }

    /**
     * MultipartFile'i byte dizisine donusturur.
     * Dosyayi dogrulamak icin once validate() metodunu cagirir.
     *
     * @throws IOException           Dosya okunamazsa firlatilir.
     * @throws IllegalArgumentException Gecersiz tur veya boyut asiminda firlatilir.
     */
    public static byte[] toBytes(MultipartFile file) throws IOException {
        validate(file);
        return file.getBytes();
    }

    /**
     * Byte dizisini Base64 veri URI'sine donusturur (data URI olarak kullanilir).
     * Ornek cikti: "data:image/jpeg;base64,/9j/4AAQSkZJRg..."
     */
    public static String toDataUri(byte[] imageData, String contentType) {
        if (imageData == null || imageData.length == 0) return null;
        String type = (contentType != null) ? contentType : "image/jpeg";
        String encoded = Base64.getEncoder().encodeToString(imageData);
        return "data:" + type + ";base64," + encoded;
    }

    /**
     * Byte dizisini saf Base64 String'e donusturur (Thymeleaf th:src icin kullanilir).
     * Kullanim: th:src="'data:image/jpeg;base64,' + ${imageBase64}"
     */
    public static String toBase64(byte[] imageData) {
        if (imageData == null || imageData.length == 0) return null;
        return Base64.getEncoder().encodeToString(imageData);
    }

    /**
     * Dosya turunu ve boyutunu dogrular.
     * MIME turu izin verilenler listesinde degilse veya boyut 5 MB'i asiyorsa
     * IllegalArgumentException firlatir.
     */
    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Dosya bos olamaz.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Gecersiz dosya turu. Izin verilen turler: JPEG, PNG, WEBP, GIF");
        }
        final long MAX_SIZE = 5L * 1024 * 1024; // 5 MB
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException(
                    "Dosya boyutu 5 MB sinirini asiyor. (Yuklenen: "
                            + String.format("%.2f", file.getSize() / (1024.0 * 1024)) + " MB)");
        }
    }

    /**
     * Dosyanin bir goruntu olup olmadigini kontrol eder.
     * Null veya bos dosya icin false doner.
     */
    public static boolean isImage(MultipartFile file) {
        if (file == null || file.isEmpty()) return false;
        String type = file.getContentType();
        return type != null && ALLOWED_TYPES.contains(type.toLowerCase());
    }
}

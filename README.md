# CityCare — Akilli Sehir Ihbar Sistemi

## Proje Ozeti

CityCare, vatandaslarin sehirdeki sorunlari fotograf ekleyerek bildirebildigi,
yetkililerin ise bu ihbarlari yonetebildigi bir Spring Boot web uygulamasidir.

---

## Proje Yapisi

```
citycare/
├── pom.xml
├── src/
│   └── main/
│       ├── java/com/citycare/
│       │   ├── CityCareApplication.java          # Uygulamanin baslangic noktasi
│       │   ├── config/
│       │   │   ├── SecurityConfig.java           # Spring Security yapılandirmasi
│       │   │   └── DataInitializer.java          # Baslangicta demo hesaplari olusturur
│       │   ├── controller/
│       │   │   ├── AuthController.java           # Giris / Kayit sayfalari
│       │   │   ├── ReportController.java         # CRUD + arama endpoint'leri
│       │   │   ├── AdminController.java          # Admin ihbar + kullanici yonetimi
│       │   │   ├── ImageController.java          # /images/report/{id} -> BLOB -> HTTP
│       │   │   └── GlobalExceptionHandler.java   # 404 / 403 / hata yonetimi
│       │   ├── dto/
│       │   │   ├── ReportDTO.java                # Ihbar form baglama nesnesi
│       │   │   └── StatusUpdateDTO.java          # Admin durum + geri bildirim formu
│       │   ├── entity/
│       │   │   ├── User.java                     # Kullanici varligi (enabled bayragi dahil)
│       │   │   ├── Report.java                   # Ihbar varligi (LONGBLOB resim + admin geri bildirim)
│       │   │   ├── Role.java                     # USER / ADMIN
│       │   │   ├── ReportCategory.java           # Yol, Park, Cevre vb.
│       │   │   └── ReportStatus.java             # Acik, Incelemede, Cozuldu, Reddedildi
│       │   ├── repository/
│       │   │   ├── UserRepository.java           # Kullanici veri erisim katmani
│       │   │   └── ReportRepository.java         # Dinamik JPQL arama sorgulari
│       │   ├── service/
│       │   │   ├── UserService.java              # Kullanici is mantigi
│       │   │   ├── UserDetailsServiceImpl.java   # Spring Security entegrasyonu
│       │   │   └── ReportService.java            # Ihbar is mantigi katmani
│       │   └── util/
│       │       └── ImageUtil.java               # Base64 donusum yardimcisi
│       └── resources/
│           ├── application.properties
│           └── templates/
│               ├── fragments/layout.html         # Navbar + sidebar fragment
│               ├── auth/
│               │   ├── login.html
│               │   └── register.html
│               ├── reports/
│               │   ├── list.html                 # Tum ihbarlar + arama/filtre
│               │   ├── my-reports.html           # Vatandas kendi ihbarlari
│               │   ├── create.html               # Yeni ihbar formu
│               │   ├── detail.html               # Detay + admin geri bildirim formu
│               │   └── edit.html                 # Ihbar duzenleme formu
│               ├── admin/
│               │   ├── dashboard.html            # Admin panosu
│               │   ├── reports.html              # Admin ihbar yonetimi
│               │   └── users.html                # Kullanici yonetimi
│               └── error/
│                   ├── not-found.html
│                   └── forbidden.html
```

---

## Kurulum ve Calistirma

### 1. Gereksinimler

| Arac       | Surumu | Kontrol         |
|------------|--------|-----------------|
| Java JDK   | 17+    | `java -version` |
| Maven      | 3.8+   | `mvn -version`  |
| MySQL      | 8.0+   | `mysql --version` |

### 2. MySQL Veritabani Olusturma

MySQL Workbench veya komut satiri uzerinden asagidaki sorguyu calistirin:

```sql
CREATE DATABASE citycare_db CHARACTER SET utf8mb4 COLLATE utf8mb4_turkish_ci;
```

Bu tek sorgu yeterlidir. Tablolar Spring Boot tarafindan otomatik olusturulur (`ddl-auto=update`).

### 3. application.properties Yapılandirmasi

`src/main/resources/application.properties` dosyasindaki sifre bilgisini kendi MySQL sifrenizle degistirin:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/citycare_db?useSSL=false&serverTimezone=Europe/Istanbul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=1234
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

### 4. Uygulamayi Baslat

```bash
mvn spring-boot:run
```

### 5. Tarayicida Ac

```
http://localhost:8080
```

---

## Varsayilan Hesaplar

`DataInitializer` sinifi uygulama ilk calıstiginda asagidaki hesaplari otomatik olusturur:

| Rol        | Kullanici Adi | Sifre         |
|------------|---------------|---------------|
| ADMIN      | admin         | admin123      |
| VATANDAS   | vatandas      | vatandas123   |

**Onemli:** Production ortaminda bu sifreleri degistirin!

---

## Ozellikler

| Ozellik                      | Aciklama                                                           |
|------------------------------|--------------------------------------------------------------------|
| Spring Security              | Giris/Cikis, CSRF korumasi, rol tabanli URL erisim kontrolu        |
| BLOB Resim Depolama          | Resimler MySQL LONGBLOB olarak saklanir                            |
| ImageController              | `/images/report/{id}` endpoint'i BLOB veriyi HTTP yaniti olarak sunar |
| Dinamik Arama                | Baslik, kategori, durum ve aciliyet filtresi; JPQL ile             |
| Sayfalama                    | Spring Data Page ile 9'lu/15'li sayfa gosterimi                    |
| CRUD                         | Olustur, Listele, Detay, Duzenle, Sil                             |
| Admin Geri Bildirim          | Durum guncelleme + opsiyonel aciklama + surukle-birak fotograf     |
| Aciliyet Sistemi             | 1-5 puan arasi renk kodlu balon gostergesi                        |
| Konum Bilgisi                | Serbest metin adres veya koordinat girisi                          |
| Kullanici Engelleme          | Admin vatandas hesaplarini pasif hale getirebilir                  |
| Tema Ayrimi                  | Vatandas (mavi) ve admin (koyu antrasit) farkli tema               |
| Tam Turkce Arayuz            | Tum buton, etiket ve mesajlar Turkce                               |

---

## Resim Isleme — Kullanilan Strateji

Bu projede **ImageController** stratejisi kullanilmaktadir:

```html
<!-- Vatandas resmi -->
<img th:src="@{/images/report/{id}(id=${report.id})}"/>

<!-- Admin cozum resmi -->
<img th:src="@{/images/feedback/{id}(id=${report.id})}"/>
```

`ImageController`, ilgili ihbarin `imageData` veya `adminImageData` alanini
MySQL LONGBLOB'dan okuyarak `ResponseEntity<byte[]>` olarak sunar.
Tarayici onbelleklemesi icin `Cache-Control: max-age=300` basliği eklenir.

---

## Guvenlik Akisi

```
Kullanici istegi
    |
    v
SecurityFilterChain
    |-- /login, /register, /css/**, /images/** --> Herkese acik
    |-- /admin/**                              --> Yalnizca ROLE_ADMIN
    `-- Diger tum yollar                       --> Kimlik dogrulanmis kullanici
              |
              v
    UserDetailsServiceImpl.loadUserByUsername()
              |
              v
    BCrypt sifre dogrulamasi
              |
    +---------+-----------+
    |                     |
  Basarili             Basarisiz / Engelli
    |                     |
    v                     v
/reports              /login?error=true
```

---

## MySQL Tablo Yapisi (Referans)

Tablolar `spring.jpa.hibernate.ddl-auto=update` ile otomatik olusturulur.
Asagidaki tablo yapisi referans amaciyla verilmistir:

```sql
CREATE TABLE users (
    id       BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50)  NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(20)  NOT NULL,
    enabled  TINYINT(1)   NOT NULL DEFAULT 1
);

CREATE TABLE reports (
    id                   BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title                VARCHAR(150)  NOT NULL,
    description          TEXT          NOT NULL,
    category             VARCHAR(30)   NOT NULL,
    urgency              INT           NOT NULL DEFAULT 3,
    location             VARCHAR(300),
    image_data           LONGBLOB,
    image_content_type   VARCHAR(50),
    status               VARCHAR(20)   NOT NULL DEFAULT 'ACIK',
    admin_note           TEXT,
    admin_image_data     LONGBLOB,
    admin_image_content_type VARCHAR(50),
    feedback_at          DATETIME,
    created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id              BIGINT        NOT NULL,
    CONSTRAINT fk_reports_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

---

## Sik Karsılasilan Hatalar

| Hata                              | Sebebi                          | Cozum                                          |
|-----------------------------------|---------------------------------|------------------------------------------------|
| `Communications link failure`     | MySQL servisi calismiyor        | MySQL servisini baslatin                       |
| `Access denied for user 'root'`   | Yanlis sifre                    | `application.properties` sifresini kontrol et |
| `Unknown database 'citycare_db'`  | Veritabani olusturulmamis       | `CREATE DATABASE citycare_db` sorgusunu calistir |
| Port 8080 mesgul                  | Baska uygulama ayni portu kullaniyor | `server.port=8081` ekle                   |
| `Template parsing error`          | Eski ZIP dosyasi kullaniliyor   | En son ZIP'i indirip tekrar deneyin            |

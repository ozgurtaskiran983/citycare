package com.citycare.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

/**
 * User - Sisteme kayitli kullaniciyi temsil eden JPA varlik sinifi.
 *
 * Roller:
 *   ADMIN : Ihbarlari yonetebilir (durum, aciliyet, silme) ve kullanicilari engelleyebilir.
 *   USER  : Yeni ihbar olusturabilir ve kendi ihbarlarini duzenleyip silebilir.
 *
 * enabled alani giris iznini kontrol eder:
 *   true  -> Hesap aktif, giris yapilabilir.
 *   false -> Hesap admin tarafindan engellenmis, giris reddedilir.
 *            Bu kontrol UserDetailsServiceImpl.loadUserByUsername() icinde yapilir.
 *
 * reports alani tek-yazarli/cok-ihbarli iliskiyi (OneToMany) temsil eder.
 * CascadeType.ALL ile kullanici silindiginde ilgili ihbarlar da silinir.
 */
@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Kullanıcı adı boş olamaz")
    @Size(min = 3, max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank(message = "Şifre boş olamaz")
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /**
     * Hesap durum bayragi.
     * Admin bu alani false yaparak vatandasin sisteme girisini engelleyebilir.
     * Spring Security kimlik dogrulamasi sirasinda UserDetailsServiceImpl tarafindan kontrol edilir.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /** Tek yonlu bir-cok iliskisi: bir kullanicinin birden fazla ihrabi olabilir. */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Report> reports = new ArrayList<>();
}

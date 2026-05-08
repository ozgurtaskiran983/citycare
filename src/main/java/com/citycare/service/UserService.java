package com.citycare.service;

import com.citycare.entity.Role;
import com.citycare.entity.User;
import com.citycare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * UserService - Kullanici hesabi yonetimi icin is mantigi katmani.
 *
 * Sorumluluklar:
 *   - Kullanici adina veya ID'ye gore arama (guvenlik katmani ve controller'lar tarafindan kullanilir)
 *   - BCrypt ile sifrelenmis yeni hesap olusturma
 *   - Vatandas hesaplarini etkinlestirme/engelleme (admin islemi)
 *   - Admin paneli icin istatistik sayimlari saglama
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Kullaniciyi kullanici adina gore bulur.
     * Bulunamazsa RuntimeException firlatir (GlobalExceptionHandler 404 sayfasina yonlendirir).
     * ReportService ve controller'larda kimlik dogrulanmis kullanici adlarini cozumlemek icin kullanilir.
     */
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + username));
    }

    /**
     * Kullaniciyi birincil anahtara gore bulur.
     * Bulunamazsa RuntimeException firlatir (GlobalExceptionHandler 404 sayfasina yonlendirir).
     * AdminController'da toggle ve detay islemleri icin kullanilir.
     */
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: ID=" + id));
    }

    /**
     * ROLE_USER rolune sahip tum kullanicilari (vatandaslari) alfabetik siraya gore getirir.
     * Admin hesaplari bu listeden cikarilir.
     */
    @Transactional(readOnly = true)
    public List<User> findAllCitizens() {
        return userRepository.findByRoleNotOrderByUsernameAsc(Role.ADMIN);
    }

    /**
     * BCrypt ile sifrelenmis yeni bir hesap olusturur.
     * Kullanici adi zaten alinmissa RuntimeException firlatir.
     */
    @Transactional
    public User register(String username, String rawPassword, Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Bu kullanıcı adı zaten kullanılıyor: " + username);
        }
        return userRepository.save(User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .enabled(true)
                .build());
    }

    /** Verilen kullanici adinin sistemde kayitli olup olmadigini dondurur. */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Kullanici hesabinin enabled bayragini gunceller.
     * false = engellendi (giris basarisiz olur), true = aktif.
     * AdminController'daki toggle endpoint'i tarafindan cagrilir.
     */
    @Transactional
    public void setEnabled(Long userId, boolean enabled) {
        User user = findById(userId);
        user.setEnabled(enabled);
        userRepository.save(user);
    }

    /** Sistemdeki toplam kullanici sayisini dondurur. */
    @Transactional(readOnly = true)
    public long countAll() { return userRepository.count(); }

    /** enabled=true olan kullanici hesaplarinin sayisini dondurur. */
    @Transactional(readOnly = true)
    public long countEnabled() {
        return userRepository.findAll().stream().filter(User::isEnabled).count();
    }
}

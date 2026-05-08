package com.citycare.service;

import com.citycare.entity.User;
import com.citycare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * UserDetailsServiceImpl - Spring Security kimlik dogrulama entegrasyonu.
 *
 * Her giris denemesinde DaoAuthenticationProvider tarafindan cagrilir.
 * User varligini kullanici adina gore yukler ve Spring Security'nin
 * kullandigi UserDetails nesnesine sarar. Bu nesne asagidakileri icerir:
 *   - Sifre ozeti (BCrypt karsilastirmasi icin)
 *   - enabled bayragi (false = giris DisabledException ile reddedilir)
 *   - Yetki bilgisi (ROLE_USER veya ROLE_ADMIN)
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Spring Security kimlik dogrulamasi icin kullaniciyi kullanici adina gore yukler.
     * enabled bayragi hesabin giris yapip yapamayacagini belirler:
     *   enabled=true  -> Giris islemi devam eder.
     *   enabled=false -> Spring Security DisabledException firlatir, giris engellenir.
     *
     * @throws UsernameNotFoundException Verilen kullanici adi sistemde kayitli degilse firlatilir.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Kullanici bulunamadi: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),   // hesap aktif mi (false -> giris engellenir)
                true,               // hesap suresi dolmadi
                true,               // sifre suresi dolmadi
                true,               // hesap kilitli degil
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}

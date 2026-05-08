package com.citycare.repository;

import com.citycare.entity.Role;
import com.citycare.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * UserRepository - User varliклари icin Spring Data JPA deposu.
 *
 * Sagladigi ozel metotlar:
 *   findByUsername   : Kimlik dogrulama sirasinda UserDetailsServiceImpl tarafindan kullanilir.
 *   existsByUsername : Kayit isleminde kullanici adi tekrarini onlemek icin kullanilir.
 *   findByRoleNot    : Admin panelinde yalnizca vatandas hesaplarini listelemek icin kullanilir.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Kullaniciya ozgu benzersiz kullanici adina gore arama yapar.
     * Eslesen kayit yoksa Optional.empty() doner; cagiran metot bu durumu isler.
     */
    Optional<User> findByUsername(String username);

    /**
     * Kullanici adinin sistemde kayitli olup olmadigini kontrol eder.
     * Kayit sirasinda mukerer hesap olusturulmasini engellemek icin kullanilir.
     */
    boolean existsByUsername(String username);

    /**
     * Belirtilen rol DISINDAKI tum kullanicilari alfabetik siraya gore getirir.
     * Role.ADMIN ile cagrildiginda yalnizca vatandas (ROLE_USER) hesaplari listelenir.
     */
    List<User> findByRoleNotOrderByUsernameAsc(Role role);
}

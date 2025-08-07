package com.example.ariaapi.repository;

import com.example.ariaapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 해시 값으로 사용자를 찾는 메소드들
    Optional<User> findByUsrNmHash(String usrNmHash);
    Optional<User> findByUsrTelHash(String usrTelHash);
    Optional<User> findByUsrBirthHash(String usrBirthHash);

}

package com.ho.account.security.repository;

import com.ho.account.security.domain.SystemUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemUserRepository extends JpaRepository<SystemUser, String> {
    Optional<SystemUser> findByUserId(String userId);
}

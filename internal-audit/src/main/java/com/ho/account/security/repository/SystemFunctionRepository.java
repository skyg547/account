package com.ho.account.security.repository;

import com.ho.account.security.domain.SystemFunction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemFunctionRepository extends JpaRepository<SystemFunction, String> {
    Optional<SystemFunction> findByFuncCode(String funcCode);
}

package com.cmpt276.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.cmpt276.project.model.Account;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Integer> {
    Optional<Account> findByEmail(String email);
}
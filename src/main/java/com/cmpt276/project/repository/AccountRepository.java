package com.cmpt276.project.repository;

import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.cmpt276.project.model.Account;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.email = :email")
    Optional<Account> lockForVisit(String email);

    @Query("""
            select salonId from Account a join a.recentSalonIds salonId
            where a.email = :email order by index(salonId)
            """)
    List<String> recentSalonIds(String email);

    Optional<Account> findByEmail(String email);
    Optional<Account> findByGoogleSubject(String googleSubject);
}
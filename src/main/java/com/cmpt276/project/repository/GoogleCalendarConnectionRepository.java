package com.cmpt276.project.repository;

import com.cmpt276.project.model.Account;
import com.cmpt276.project.model.GoogleCalendarConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

public interface GoogleCalendarConnectionRepository extends JpaRepository<GoogleCalendarConnection, Integer> {
    Optional<GoogleCalendarConnection> findByCustomer(Account customer);

    @Transactional
    void deleteByCustomer(Account customer);
}

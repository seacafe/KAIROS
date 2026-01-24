package com.kairos.trading.domain.account.repository;

import com.kairos.trading.domain.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 계좌 Repository.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * 계좌번호로 계좌 조회.
     */
    Optional<Account> findByAccountNo(String accountNo);
}

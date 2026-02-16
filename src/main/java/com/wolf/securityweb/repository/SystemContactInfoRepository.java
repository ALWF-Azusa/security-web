package com.wolf.securityweb.repository;

import com.wolf.securityweb.model.SystemContactInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SystemContactInfoRepository extends JpaRepository<SystemContactInfo, Long> {

    // 🔥 就是這行！沒有這行，Service 就會報錯找不到方法
    Optional<SystemContactInfo> findByDomainName(String domainName);
}
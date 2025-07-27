package store.piku.back.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import store.piku.back.auth.entity.AllowedEmail;

public interface AllowedEmailDomainRepository extends JpaRepository<AllowedEmail,Long> {
    boolean existsByDomain(String domain);
}

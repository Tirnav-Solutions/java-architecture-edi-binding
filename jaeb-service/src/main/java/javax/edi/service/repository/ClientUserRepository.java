package javax.edi.service.repository;

import java.util.Optional;

import javax.edi.service.entity.ClientUser;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientUserRepository extends JpaRepository<ClientUser, Long> {

    Optional<ClientUser> findByEmail(String email);

    boolean existsByEmail(String email);

    java.util.List<ClientUser> findByTenantId(String tenantId);
}

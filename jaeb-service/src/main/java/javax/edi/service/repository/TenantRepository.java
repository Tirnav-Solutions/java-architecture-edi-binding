package javax.edi.service.repository;

import java.util.Optional;

import javax.edi.service.entity.Tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByTenantId(String tenantId);

    boolean existsByTenantId(String tenantId);
}

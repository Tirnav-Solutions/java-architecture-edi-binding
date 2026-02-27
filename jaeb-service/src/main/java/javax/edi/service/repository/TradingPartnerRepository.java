package javax.edi.service.repository;

import java.util.List;
import java.util.Optional;

import javax.edi.service.entity.TradingPartner;
import javax.edi.service.entity.TradingPartner.PartnerStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradingPartnerRepository extends JpaRepository<TradingPartner, Long> {

    Optional<TradingPartner> findByPartnerId(String partnerId);

    Optional<TradingPartner> findByIsaId(String isaId);

    List<TradingPartner> findByStatus(PartnerStatus status);

    List<TradingPartner> findByStatusOrderByPartnerNameAsc(PartnerStatus status);

    boolean existsByPartnerId(String partnerId);

    boolean existsByIsaId(String isaId);

    // --- Tenant-scoped queries ---

    List<TradingPartner> findByTenantId(String tenantId);

    List<TradingPartner> findByTenantIdAndStatus(String tenantId, PartnerStatus status);

    Optional<TradingPartner> findByTenantIdAndPartnerId(String tenantId, String partnerId);

    long countByTenantId(String tenantId);

    long countByTenantIdAndStatus(String tenantId, PartnerStatus status);
}

package javax.edi.service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.edi.service.entity.TradingPartner;
import javax.edi.service.entity.TradingPartner.PartnerStatus;
import javax.edi.service.repository.TradingPartnerRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for managing trading partner onboarding and configuration.
 */
@Service
public class TradingPartnerService {

    private static final Logger LOG = LoggerFactory.getLogger(TradingPartnerService.class);

    @Autowired
    private TradingPartnerRepository repository;

    public TradingPartner createPartner(TradingPartner partner) {
        if (repository.existsByPartnerId(partner.getPartnerId())) {
            throw new IllegalArgumentException("Partner ID already exists: " + partner.getPartnerId());
        }
        partner.setCreatedAt(LocalDateTime.now());
        partner.setUpdatedAt(LocalDateTime.now());
        if (partner.getStatus() == null) {
            partner.setStatus(PartnerStatus.ONBOARDING);
        }
        TradingPartner saved = repository.save(partner);
        LOG.info("Trading partner created: {} ({})", saved.getPartnerName(), saved.getPartnerId());
        return saved;
    }

    public TradingPartner updatePartner(String partnerId, TradingPartner updates) {
        TradingPartner existing = repository.findByPartnerId(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("Partner not found: " + partnerId));

        if (updates.getPartnerName() != null) existing.setPartnerName(updates.getPartnerName());
        if (updates.getIsaId() != null) existing.setIsaId(updates.getIsaId());
        if (updates.getIsaIdQualifier() != null) existing.setIsaIdQualifier(updates.getIsaIdQualifier());
        if (updates.getGsApplicationCode() != null) existing.setGsApplicationCode(updates.getGsApplicationCode());
        if (updates.getSupportedTransactionTypes() != null) existing.setSupportedTransactionTypes(updates.getSupportedTransactionTypes());
        if (updates.getElementDelimiter() != null) existing.setElementDelimiter(updates.getElementDelimiter());
        if (updates.getSegmentDelimiter() != null) existing.setSegmentDelimiter(updates.getSegmentDelimiter());
        if (updates.getComponentDelimiter() != null) existing.setComponentDelimiter(updates.getComponentDelimiter());
        if (updates.getSftpHost() != null) existing.setSftpHost(updates.getSftpHost());
        if (updates.getSftpPort() != null) existing.setSftpPort(updates.getSftpPort());
        if (updates.getSftpUsername() != null) existing.setSftpUsername(updates.getSftpUsername());
        if (updates.getSftpPassword() != null) existing.setSftpPassword(updates.getSftpPassword());
        if (updates.getSftpInboundDir() != null) existing.setSftpInboundDir(updates.getSftpInboundDir());
        if (updates.getSftpOutboundDir() != null) existing.setSftpOutboundDir(updates.getSftpOutboundDir());
        if (updates.getSftpArchiveDir() != null) existing.setSftpArchiveDir(updates.getSftpArchiveDir());
        if (updates.getContactEmail() != null) existing.setContactEmail(updates.getContactEmail());
        if (updates.getNotes() != null) existing.setNotes(updates.getNotes());
        if (updates.getStatus() != null) existing.setStatus(updates.getStatus());
        existing.setAutoAcknowledge(updates.isAutoAcknowledge());
        existing.setUpdatedAt(LocalDateTime.now());

        return repository.save(existing);
    }

    public Optional<TradingPartner> getPartner(String partnerId) {
        return repository.findByPartnerId(partnerId);
    }

    public Optional<TradingPartner> findByIsaId(String isaId) {
        return repository.findByIsaId(isaId);
    }

    public List<TradingPartner> getAllPartners() {
        return repository.findAll();
    }

    public List<TradingPartner> getActivePartners() {
        return repository.findByStatusOrderByPartnerNameAsc(PartnerStatus.ACTIVE);
    }

    public List<TradingPartner> getPartnersWithSftp() {
        List<TradingPartner> active = getActivePartners();
        active.removeIf(p -> p.getSftpHost() == null || p.getSftpHost().isEmpty());
        return active;
    }

    public void activatePartner(String partnerId) {
        updateStatus(partnerId, PartnerStatus.ACTIVE);
    }

    public void suspendPartner(String partnerId) {
        updateStatus(partnerId, PartnerStatus.SUSPENDED);
    }

    public void deactivatePartner(String partnerId) {
        updateStatus(partnerId, PartnerStatus.INACTIVE);
    }

    private void updateStatus(String partnerId, PartnerStatus status) {
        TradingPartner partner = repository.findByPartnerId(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("Partner not found: " + partnerId));
        partner.setStatus(status);
        partner.setUpdatedAt(LocalDateTime.now());
        repository.save(partner);
        LOG.info("Partner {} status changed to {}", partnerId, status);
    }
}

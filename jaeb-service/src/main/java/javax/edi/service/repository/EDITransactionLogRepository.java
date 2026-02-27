package javax.edi.service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.edi.service.entity.EDITransactionLog;
import javax.edi.service.entity.EDITransactionLog.Direction;
import javax.edi.service.entity.EDITransactionLog.Source;
import javax.edi.service.entity.EDITransactionLog.Status;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EDITransactionLogRepository extends JpaRepository<EDITransactionLog, Long> {

    Optional<EDITransactionLog> findByTransactionId(String transactionId);

    Page<EDITransactionLog> findByPartnerIdOrderByProcessedAtDesc(String partnerId, Pageable pageable);

    Page<EDITransactionLog> findByTransactionTypeOrderByProcessedAtDesc(String transactionType, Pageable pageable);

    Page<EDITransactionLog> findByStatusOrderByProcessedAtDesc(Status status, Pageable pageable);

    Page<EDITransactionLog> findByDirectionOrderByProcessedAtDesc(Direction direction, Pageable pageable);

    List<EDITransactionLog> findByPartnerIdAndTransactionType(String partnerId, String transactionType);

    List<EDITransactionLog> findByInterchangeControlNumber(String interchangeControlNumber);

    Page<EDITransactionLog> findAllByOrderByProcessedAtDesc(Pageable pageable);

    /** Count transactions by status for dashboard. */
    long countByStatus(Status status);

    long countByPartnerId(String partnerId);

    long countByTransactionType(String transactionType);

    /** Find undelivered 997s for SFTP delivery. */
    List<EDITransactionLog> findByEdi997DeliveredFalseAndEdi997ContentIsNotNullAndPartnerIdIsNotNull();

    /** Transactions in a date range. */
    @Query("SELECT t FROM EDITransactionLog t WHERE t.processedAt BETWEEN :from AND :to ORDER BY t.processedAt DESC")
    Page<EDITransactionLog> findByDateRange(@Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to,
                                            Pageable pageable);

    // --- Tenant-scoped queries ---

    Page<EDITransactionLog> findByTenantIdOrderByProcessedAtDesc(String tenantId, Pageable pageable);

    Page<EDITransactionLog> findByTenantIdAndStatusOrderByProcessedAtDesc(String tenantId, Status status, Pageable pageable);

    Page<EDITransactionLog> findByTenantIdAndTransactionTypeOrderByProcessedAtDesc(String tenantId, String transactionType, Pageable pageable);

    Page<EDITransactionLog> findByTenantIdAndPartnerIdOrderByProcessedAtDesc(String tenantId, String partnerId, Pageable pageable);

    long countByTenantId(String tenantId);

    long countByTenantIdAndStatus(String tenantId, Status status);

    @Query("SELECT t FROM EDITransactionLog t WHERE t.tenantId = :tenantId AND t.processedAt BETWEEN :from AND :to ORDER BY t.processedAt DESC")
    Page<EDITransactionLog> findByTenantIdAndDateRange(@Param("tenantId") String tenantId,
                                                       @Param("from") LocalDateTime from,
                                                       @Param("to") LocalDateTime to,
                                                       Pageable pageable);
}

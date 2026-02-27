package javax.edi.service.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.edi.service.dto.ApiResponse;
import javax.edi.service.entity.EDITransactionLog;
import javax.edi.service.entity.EDITransactionLog.Status;
import javax.edi.service.repository.EDITransactionLogRepository;
import javax.edi.service.sftp.SftpPollingService;
import javax.edi.service.service.TradingPartnerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Transaction history, dashboard, and SFTP management API.
 */
@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "Transaction history, dashboard, and SFTP management")
public class TransactionController {

    @Autowired
    private EDITransactionLogRepository txnRepository;

    @Autowired
    private SftpPollingService sftpPollingService;

    @Autowired
    private TradingPartnerService partnerService;

    // ==================== Dashboard ====================

    @Operation(summary = "Transaction dashboard",
               description = "Summary counts of all transaction statuses, total by type, etc.")
    @GetMapping(value = "/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> dashboard() {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("totalTransactions", txnRepository.count());

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Status status : Status.values()) {
            long count = txnRepository.countByStatus(status);
            if (count > 0) byStatus.put(status.name(), count);
        }
        data.put("byStatus", byStatus);

        return ResponseEntity.ok(ApiResponse.ok(data, "Transaction dashboard"));
    }

    // ==================== Transaction History ====================

    @Operation(summary = "List all transactions (paginated)")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<EDITransactionLog> result = txnRepository.findAllByOrderByProcessedAtDesc(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(toPageMap(result), "Transactions (page " + page + ")"));
    }

    @Operation(summary = "Get transaction by tracking ID")
    @GetMapping(value = "/{transactionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> getTransaction(@PathVariable String transactionId) {
        return txnRepository.findByTransactionId(transactionId)
                .map(t -> ResponseEntity.ok(ApiResponse.ok(t)))
                .orElse(ResponseEntity.status(404)
                        .body(ApiResponse.error("Transaction not found: " + transactionId)));
    }

    @Operation(summary = "List transactions by partner")
    @GetMapping(value = "/partner/{partnerId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> byPartner(
            @PathVariable String partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<EDITransactionLog> result = txnRepository.findByPartnerIdOrderByProcessedAtDesc(
                partnerId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(toPageMap(result)));
    }

    @Operation(summary = "List transactions by type")
    @GetMapping(value = "/type/{transactionType}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> byType(
            @PathVariable String transactionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<EDITransactionLog> result = txnRepository.findByTransactionTypeOrderByProcessedAtDesc(
                transactionType, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(toPageMap(result)));
    }

    @Operation(summary = "List transactions by status")
    @GetMapping(value = "/status/{status}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> byStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Status s = Status.valueOf(status.toUpperCase());
            Page<EDITransactionLog> result = txnRepository.findByStatusOrderByProcessedAtDesc(
                    s, PageRequest.of(page, size));
            return ResponseEntity.ok(ApiResponse.ok(toPageMap(result)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid status: " + status));
        }
    }

    @Operation(summary = "List transactions by date range")
    @GetMapping(value = "/date-range", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> byDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(LocalTime.MAX);
        Page<EDITransactionLog> result = txnRepository.findByDateRange(fromDt, toDt, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(toPageMap(result)));
    }

    @Operation(summary = "Search by interchange control number")
    @GetMapping(value = "/control-number/{controlNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> byControlNumber(@PathVariable String controlNumber) {
        return ResponseEntity.ok(ApiResponse.ok(
                txnRepository.findByInterchangeControlNumber(controlNumber)));
    }

    // ==================== SFTP Operations ====================

    @Operation(summary = "Trigger SFTP poll for all active partners",
               description = "Manually triggers a poll of all active partners' SFTP servers. "
                           + "Normally this runs on a schedule, but this endpoint lets you trigger it on demand.")
    @PostMapping(value = "/sftp/poll", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> triggerSftpPoll() {
        sftpPollingService.pollAllPartners();
        return ResponseEntity.ok(ApiResponse.ok(null, "SFTP poll triggered for all active partners"));
    }

    @Operation(summary = "Trigger SFTP poll for a specific partner")
    @PostMapping(value = "/sftp/poll/{partnerId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> triggerSftpPollForPartner(@PathVariable String partnerId) {
        return partnerService.getPartner(partnerId)
                .map(partner -> {
                    sftpPollingService.pollPartner(partner);
                    return ResponseEntity.ok(ApiResponse.ok(null,
                            "SFTP poll triggered for partner: " + partnerId));
                })
                .orElse(ResponseEntity.status(404)
                        .body(ApiResponse.error("Partner not found: " + partnerId)));
    }

    // ==================== Helper ====================

    private Map<String, Object> toPageMap(Page<?> page) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("content", page.getContent());
        map.put("page", page.getNumber());
        map.put("size", page.getSize());
        map.put("totalElements", page.getTotalElements());
        map.put("totalPages", page.getTotalPages());
        map.put("hasNext", page.hasNext());
        return map;
    }
}

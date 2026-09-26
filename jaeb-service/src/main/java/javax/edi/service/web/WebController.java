package javax.edi.service.web;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import javax.edi.bind.EDIFACTUnmarshalResult;
import javax.edi.bind.EDIFACTUnmarshaller;
import javax.edi.bind.EDIUnmarshalResult;
import javax.edi.bind.EDIUnmarshaller;
//import javax.edi.model.edifact.delfor.d96a.DelforD96A;
import javax.edi.model.x12.edi850.PurchaseOrder;
import javax.edi.service.entity.ClientUser;
import javax.edi.service.entity.CommunicationChannel;
import javax.edi.service.entity.EDITransactionLog;
import javax.edi.service.entity.EDITransactionLog.Status;
import javax.edi.service.entity.TradingPartner;
import javax.edi.service.entity.TradingPartner.PartnerStatus;
import javax.edi.service.repository.ClientUserRepository;
import javax.edi.service.repository.CommunicationChannelRepository;
import javax.edi.service.repository.EDITransactionLogRepository;
import javax.edi.service.repository.TradingPartnerRepository;
import javax.edi.service.service.AuthService;
import javax.edi.service.service.TradingPartnerService;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Multi-tenant Thymeleaf web controller.
 * All data is scoped to the logged-in user's tenant.
 * Super-admins (ADMIN role) see all tenants' data.
 */
@Controller
@RequestMapping("/ui")
public class WebController {

    @Autowired private EDITransactionLogRepository txnRepo;
    @Autowired private TradingPartnerRepository partnerRepo;
    @Autowired private CommunicationChannelRepository channelRepo;
    @Autowired private ClientUserRepository userRepo;
    @Autowired private TradingPartnerService partnerService;
    @Autowired private AuthService authService;
    @Autowired private javax.edi.service.channel.ChannelTestService channelTestService;
    @Autowired private javax.edi.service.registry.EDIModelRegistry modelRegistry;

    // ======================== AUTH ========================

    @GetMapping("/login")
    public String loginPage() { return "login"; }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password,
                        HttpSession session, Model model) {
        return authService.authenticate(email, password)
                .map(user -> {
                    session.setAttribute("user", user);
                    return "redirect:/ui/dashboard";
                })
                .orElseGet(() -> {
                    model.addAttribute("error", "Invalid email or password");
                    return "login";
                });
    }

    @GetMapping("/signup")
    public String signupPage() { return "signup"; }

    @PostMapping("/signup")
    public String signup(@RequestParam String tenantId, @RequestParam String companyName,
                         @RequestParam String fullName, @RequestParam String email,
                         @RequestParam String password, RedirectAttributes ra) {
        try {
            authService.registerTenant(tenantId, companyName, email, fullName, password);
            ra.addFlashAttribute("message", "Account created! Sign in below.");
            return "redirect:/ui/login";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/ui/signup";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/ui/login";
    }

    // ======================== DASHBOARD ========================

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(HttpSession session, Model model) {
        ClientUser user = currentUser(session);
        String tid = tenantScope(user);
        model.addAttribute("activeTab", "dashboard");
        model.addAttribute("currentUser", user);

        if (tid == null) {
            // Super-admin sees all
            model.addAttribute("totalTransactions", txnRepo.count());
            model.addAttribute("countParsed", txnRepo.countByStatus(Status.PARSED));
            model.addAttribute("countErrors", txnRepo.countByStatus(Status.PARSED_WITH_ERRORS));
            model.addAttribute("countFailed", txnRepo.countByStatus(Status.FAILED) + txnRepo.countByStatus(Status.ERROR));
            model.addAttribute("countAcknowledged", txnRepo.countByStatus(Status.ACKNOWLEDGED) + txnRepo.countByStatus(Status.DELIVERED));
            model.addAttribute("activePartners", partnerRepo.findByStatus(PartnerStatus.ACTIVE).size());
            model.addAttribute("recentTransactions", txnRepo.findAllByOrderByProcessedAtDesc(PageRequest.of(0, 15)).getContent());
            model.addAttribute("channels", channelRepo.findAll());
        } else {
            model.addAttribute("totalTransactions", txnRepo.countByTenantId(tid));
            model.addAttribute("countParsed", txnRepo.countByTenantIdAndStatus(tid, Status.PARSED));
            model.addAttribute("countErrors", txnRepo.countByTenantIdAndStatus(tid, Status.PARSED_WITH_ERRORS));
            model.addAttribute("countFailed", txnRepo.countByTenantIdAndStatus(tid, Status.FAILED) + txnRepo.countByTenantIdAndStatus(tid, Status.ERROR));
            model.addAttribute("countAcknowledged", txnRepo.countByTenantIdAndStatus(tid, Status.ACKNOWLEDGED) + txnRepo.countByTenantIdAndStatus(tid, Status.DELIVERED));
            model.addAttribute("activePartners", partnerRepo.countByTenantIdAndStatus(tid, PartnerStatus.ACTIVE));
            model.addAttribute("recentTransactions", txnRepo.findByTenantIdOrderByProcessedAtDesc(tid, PageRequest.of(0, 15)).getContent());
            model.addAttribute("channels", channelRepo.findByTenantId(tid));
        }
        return "dashboard";
    }

    // ======================== TRANSACTIONS ========================

    @GetMapping("/transactions")
    public String transactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String partner,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpSession session, Model model) {

        ClientUser user = currentUser(session);
        String tid = tenantScope(user);
        model.addAttribute("activeTab", "transactions");
        model.addAttribute("currentUser", user);
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterType", type);
        model.addAttribute("filterPartner", partner);
        model.addAttribute("filterFrom", from);
        model.addAttribute("filterTo", to);

        PageRequest pageable = PageRequest.of(page, size);
        Page<EDITransactionLog> txnPage;

        if (status != null && !status.isEmpty()) {
            try {
                Status s = Status.valueOf(status);
                txnPage = tid == null ? txnRepo.findByStatusOrderByProcessedAtDesc(s, pageable)
                                      : txnRepo.findByTenantIdAndStatusOrderByProcessedAtDesc(tid, s, pageable);
            } catch (IllegalArgumentException e) {
                txnPage = tid == null ? txnRepo.findAllByOrderByProcessedAtDesc(pageable)
                                      : txnRepo.findByTenantIdOrderByProcessedAtDesc(tid, pageable);
            }
        } else if (type != null && !type.isEmpty()) {
            txnPage = tid == null ? txnRepo.findByTransactionTypeOrderByProcessedAtDesc(type, pageable)
                                  : txnRepo.findByTenantIdAndTransactionTypeOrderByProcessedAtDesc(tid, type, pageable);
        } else if (partner != null && !partner.isEmpty()) {
            txnPage = tid == null ? txnRepo.findByPartnerIdOrderByProcessedAtDesc(partner, pageable)
                                  : txnRepo.findByTenantIdAndPartnerIdOrderByProcessedAtDesc(tid, partner, pageable);
        } else if (from != null && to != null) {
            txnPage = tid == null ? txnRepo.findByDateRange(from.atStartOfDay(), to.atTime(LocalTime.MAX), pageable)
                                  : txnRepo.findByTenantIdAndDateRange(tid, from.atStartOfDay(), to.atTime(LocalTime.MAX), pageable);
        } else {
            txnPage = tid == null ? txnRepo.findAllByOrderByProcessedAtDesc(pageable)
                                  : txnRepo.findByTenantIdOrderByProcessedAtDesc(tid, pageable);
        }

        model.addAttribute("txnPage", txnPage);
        model.addAttribute("pageNumbers", buildPageNumbers(txnPage.getNumber(), txnPage.getTotalPages()));
        return "transactions";
    }

    @GetMapping("/transactions/export")
    public org.springframework.http.ResponseEntity<byte[]> exportTransactionsCsv(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String partner,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpSession session) {

        ClientUser user = currentUser(session);
        String tid = tenantScope(user);
        PageRequest pageable = PageRequest.of(0, 10000); // max export rows
        Page<EDITransactionLog> txnPage;

        if (status != null && !status.isEmpty()) {
            try {
                Status s = Status.valueOf(status);
                txnPage = tid == null ? txnRepo.findByStatusOrderByProcessedAtDesc(s, pageable)
                                      : txnRepo.findByTenantIdAndStatusOrderByProcessedAtDesc(tid, s, pageable);
            } catch (IllegalArgumentException e) {
                txnPage = tid == null ? txnRepo.findAllByOrderByProcessedAtDesc(pageable)
                                      : txnRepo.findByTenantIdOrderByProcessedAtDesc(tid, pageable);
            }
        } else if (type != null && !type.isEmpty()) {
            txnPage = tid == null ? txnRepo.findByTransactionTypeOrderByProcessedAtDesc(type, pageable)
                                  : txnRepo.findByTenantIdAndTransactionTypeOrderByProcessedAtDesc(tid, type, pageable);
        } else if (partner != null && !partner.isEmpty()) {
            txnPage = tid == null ? txnRepo.findByPartnerIdOrderByProcessedAtDesc(partner, pageable)
                                  : txnRepo.findByTenantIdAndPartnerIdOrderByProcessedAtDesc(tid, partner, pageable);
        } else if (from != null && to != null) {
            txnPage = tid == null ? txnRepo.findByDateRange(from.atStartOfDay(), to.atTime(LocalTime.MAX), pageable)
                                  : txnRepo.findByTenantIdAndDateRange(tid, from.atStartOfDay(), to.atTime(LocalTime.MAX), pageable);
        } else {
            txnPage = tid == null ? txnRepo.findAllByOrderByProcessedAtDesc(pageable)
                                  : txnRepo.findByTenantIdOrderByProcessedAtDesc(tid, pageable);
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Tracking ID,Type,Direction,Partner,Source,Status,Segments,Parse Time (ms),ISA Control #,997 Code,Processed At\n");
        for (EDITransactionLog txn : txnPage.getContent()) {
            csv.append(escapeCsv(txn.getTransactionId())).append(',');
            csv.append(escapeCsv(txn.getTransactionType())).append(',');
            csv.append(txn.getDirection()).append(',');
            csv.append(escapeCsv(txn.getPartnerId())).append(',');
            csv.append(txn.getSource()).append(',');
            csv.append(txn.getStatus()).append(',');
            csv.append(txn.getSegmentCount()).append(',');
            csv.append(txn.getParseTimeMs()).append(',');
            csv.append(escapeCsv(txn.getInterchangeControlNumber())).append(',');
            csv.append(escapeCsv(txn.getAcknowledgeCode())).append(',');
            csv.append(txn.getProcessedAt() != null ? txn.getProcessedAt().toString() : "");
            csv.append('\n');
        }

        byte[] body = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String filename = "edi-transactions-" + LocalDate.now() + ".csv";
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .header("Content-Type", "text/csv; charset=UTF-8")
                .body(body);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @GetMapping("/transactions/{transactionId}")
    public String transactionDetail(@PathVariable String transactionId, HttpSession session, Model model) {
        model.addAttribute("activeTab", "transactions");
        model.addAttribute("currentUser", currentUser(session));
        return txnRepo.findByTransactionId(transactionId)
                .map(txn -> { model.addAttribute("txn", txn); return "transaction-detail"; })
                .orElse("redirect:/ui/transactions");
    }

    @PostMapping("/transactions/{transactionId}/reprocess")
    public String reprocessTransaction(@PathVariable String transactionId, RedirectAttributes ra) {
        return txnRepo.findByTransactionId(transactionId)
                .map(txn -> {
                    if (txn.getRawEdiContent() == null || txn.getRawEdiContent().isEmpty()) {
                        ra.addFlashAttribute("error", "No raw EDI content to reprocess");
                        return "redirect:/ui/transactions/" + transactionId;
                    }
                    try {
                        String rawEdi = txn.getRawEdiContent();
                        String txnType = txn.getTransactionType();
                        Class<?> modelClass = modelRegistry.resolve(txnType);
                        if (modelClass == null) {
                            ra.addFlashAttribute("error", "Unknown transaction type: " + txnType);
                            return "redirect:/ui/transactions/" + transactionId;
                        }

                        javax.edi.bind.EDIUnmarshalResult<?> result =
                                javax.edi.bind.EDIUnmarshaller.unmarshalResult(modelClass, new java.io.StringReader(rawEdi));

                        txn.setParsed(result.isParsed());
                        txn.setValid(result.isValid());
                        txn.setSegmentCount(result.getSegmentCount());
                        txn.setParseTimeMs(result.getParseTimeMillis());

                        if (result.getData() != null) {
                            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                            txn.setParsedJson(om.writeValueAsString(result.getData()));
                        }
                        if (result.hasErrors()) {
                            txn.setValidationErrorCount(result.getErrors().size());
                            StringBuilder errSb = new StringBuilder();
                            for (javax.edi.bind.util.EDIValidationError err : result.getErrors()) {
                                if (errSb.length() > 0) errSb.append("\n");
                                errSb.append(err.toString());
                            }
                            txn.setValidationErrors(errSb.toString());
                            txn.setStatus(Status.PARSED_WITH_ERRORS);
                        } else if (result.isParsed()) {
                            txn.setValidationErrorCount(0);
                            txn.setValidationErrors(null);
                            txn.setStatus(Status.PARSED);
                        } else {
                            txn.setStatus(Status.FAILED);
                        }
                        txn.setErrorMessage(null);
                        txn.setProcessedAt(java.time.LocalDateTime.now());
                        txnRepo.save(txn);
                        ra.addFlashAttribute("message", "Transaction reprocessed Ã¢ÂÂ Status: " + txn.getStatus());
                    } catch (Exception e) {
                        txn.setStatus(Status.ERROR);
                        txn.setErrorMessage(e.getMessage());
                        txnRepo.save(txn);
                        ra.addFlashAttribute("error", "Reprocess failed: " + e.getMessage());
                    }
                    return "redirect:/ui/transactions/" + transactionId;
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("error", "Transaction not found");
                    return "redirect:/ui/transactions";
                });
    }

    @GetMapping("/transactions/{transactionId}/download")
    public org.springframework.http.ResponseEntity<byte[]> downloadRawEdi(@PathVariable String transactionId) {
        return txnRepo.findByTransactionId(transactionId)
                .filter(txn -> txn.getRawEdiContent() != null)
                .map(txn -> {
                    byte[] body = txn.getRawEdiContent().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    String filename = txn.getTransactionType() + "_" + txn.getTransactionId() + ".edi";
                    return org.springframework.http.ResponseEntity.ok()
                            .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                            .header("Content-Type", "application/edi-x12")
                            .body(body);
                })
                .orElse(org.springframework.http.ResponseEntity.notFound().build());
    }

    @GetMapping("/transactions/{transactionId}/download997")
    public org.springframework.http.ResponseEntity<byte[]> download997(@PathVariable String transactionId) {
        return txnRepo.findByTransactionId(transactionId)
                .filter(txn -> txn.getEdi997Content() != null)
                .map(txn -> {
                    byte[] body = txn.getEdi997Content().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    String filename = "997_" + txn.getTransactionId() + ".edi";
                    return org.springframework.http.ResponseEntity.ok()
                            .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                            .header("Content-Type", "application/edi-x12")
                            .body(body);
                })
                .orElse(org.springframework.http.ResponseEntity.notFound().build());
    }

    // ======================== PARTNERS ========================

    @GetMapping("/partners")
    public String partners(@RequestParam(required = false) String status,
                           HttpSession session, Model model) {
        ClientUser user = currentUser(session);
        String tid = tenantScope(user);
        model.addAttribute("activeTab", "partners");
        model.addAttribute("currentUser", user);
        model.addAttribute("filterStatus", status);

        List<TradingPartner> partners;
        if (status != null && !status.isEmpty()) {
            try {
                PartnerStatus ps = PartnerStatus.valueOf(status);
                partners = tid == null ? partnerRepo.findByStatus(ps) : partnerRepo.findByTenantIdAndStatus(tid, ps);
            } catch (IllegalArgumentException e) {
                partners = tid == null ? partnerRepo.findAll() : partnerRepo.findByTenantId(tid);
            }
        } else {
            partners = tid == null ? partnerRepo.findAll() : partnerRepo.findByTenantId(tid);
        }
        model.addAttribute("partners", partners);
        return "partners";
    }

    @GetMapping("/partners/new")
    public String newPartnerForm(HttpSession session, Model model) {
        model.addAttribute("activeTab", "partners");
        model.addAttribute("currentUser", currentUser(session));
        model.addAttribute("isEdit", false);
        model.addAttribute("partner", new TradingPartner());
        model.addAttribute("channel", new CommunicationChannel());
        return "partner-form";
    }

    @PostMapping("/partners/new")
    public String createPartner(TradingPartner partner,
                                @RequestParam(value = "ch_protocol", required = false) String chProtocol,
                                @RequestParam(value = "ch_channelName", required = false) String chChannelName,
                                @RequestParam(value = "ch_direction", required = false) String chDirection,
                                @RequestParam(value = "ch_status", required = false) String chStatus,
                                @RequestParam(value = "ch_host", required = false) String chHost,
                                @RequestParam(value = "ch_port", required = false) Integer chPort,
                                @RequestParam(value = "ch_username", required = false) String chUsername,
                                @RequestParam(value = "ch_password", required = false) String chPassword,
                                @RequestParam(value = "ch_remoteDirectory", required = false) String chRemoteDirectory,
                                @RequestParam(value = "ch_archiveDirectory", required = false) String chArchiveDirectory,
                                @RequestParam(value = "ch_filePattern", required = false) String chFilePattern,
                                @RequestParam(value = "ch_passiveMode", required = false) boolean chPassiveMode,
                                @RequestParam(value = "ch_useTls", required = false) boolean chUseTls,
                                @RequestParam(value = "ch_as2LocalId", required = false) String chAs2LocalId,
                                @RequestParam(value = "ch_as2PartnerId", required = false) String chAs2PartnerId,
                                @RequestParam(value = "ch_as2Url", required = false) String chAs2Url,
                                @RequestParam(value = "ch_as2MdnMode", required = false) String chAs2MdnMode,
                                @RequestParam(value = "ch_as2MicAlgorithm", required = false) String chAs2MicAlgorithm,
                                @RequestParam(value = "ch_as2EncryptionAlgorithm", required = false) String chAs2EncAlg,
                                @RequestParam(value = "ch_as2SignMessages", required = false) boolean chAs2Sign,
                                @RequestParam(value = "ch_as2EncryptMessages", required = false) boolean chAs2Encrypt,
                                @RequestParam(value = "ch_as2RequestSignedMdn", required = false) boolean chAs2SignedMdn,
                                @RequestParam(value = "ch_as2LocalCertificate", required = false) String chAs2LocalCert,
                                @RequestParam(value = "ch_as2LocalPrivateKey", required = false) String chAs2LocalKey,
                                @RequestParam(value = "ch_as2PartnerCertificate", required = false) String chAs2PartnerCert,
                                @RequestParam(value = "ch_httpUrl", required = false) String chHttpUrl,
                                @RequestParam(value = "ch_httpMethod", required = false) String chHttpMethod,
                                @RequestParam(value = "ch_httpContentType", required = false) String chHttpContentType,
                                @RequestParam(value = "ch_httpAuthHeader", required = false) String chHttpAuthHeader,
                                @RequestParam(value = "ch_pollIntervalMs", required = false, defaultValue = "0") long chPollMs,
                                @RequestParam(value = "ch_connectionTimeoutMs", required = false, defaultValue = "10000") int chTimeoutMs,
                                HttpSession session, RedirectAttributes ra) {
        try {
            ClientUser user = currentUser(session);
            String tid = tenantScope(user);
            partner.setTenantId(tid != null ? tid : "system");
            TradingPartner created = partnerService.createPartner(partner);

            // Create communication channel if a protocol was selected
            if (chProtocol != null && !chProtocol.isEmpty()) {
                CommunicationChannel ch = new CommunicationChannel();
                ch.setTenantId(created.getTenantId());
                ch.setPartnerId(created.getPartnerId());
                ch.setChannelName(chChannelName != null && !chChannelName.isEmpty()
                        ? chChannelName : created.getPartnerName() + " " + chProtocol);
                ch.setProtocol(CommunicationChannel.Protocol.valueOf(chProtocol));
                ch.setDirection(chDirection != null ? CommunicationChannel.Direction.valueOf(chDirection)
                        : CommunicationChannel.Direction.INBOUND);
                ch.setStatus(chStatus != null ? CommunicationChannel.ChannelStatus.valueOf(chStatus)
                        : CommunicationChannel.ChannelStatus.ACTIVE);

                // SFTP / FTP
                ch.setHost(chHost);
                ch.setPort(chPort);
                ch.setUsername(chUsername);
                ch.setPassword(chPassword);
                ch.setRemoteDirectory(chRemoteDirectory);
                ch.setArchiveDirectory(chArchiveDirectory);
                ch.setFilePattern(chFilePattern);
                ch.setPassiveMode(chPassiveMode);
                ch.setUseTls(chUseTls);

                // AS2
                ch.setAs2LocalId(chAs2LocalId);
                ch.setAs2PartnerId(chAs2PartnerId);
                ch.setAs2Url(chAs2Url);
                ch.setAs2MdnMode(chAs2MdnMode);
                ch.setAs2MicAlgorithm(chAs2MicAlgorithm);
                ch.setAs2EncryptionAlgorithm(chAs2EncAlg);
                ch.setAs2SignMessages(chAs2Sign);
                ch.setAs2EncryptMessages(chAs2Encrypt);
                ch.setAs2RequestSignedMdn(chAs2SignedMdn);
                ch.setAs2LocalCertificate(chAs2LocalCert);
                ch.setAs2LocalPrivateKey(chAs2LocalKey);
                ch.setAs2PartnerCertificate(chAs2PartnerCert);

                // HTTP
                ch.setHttpUrl(chHttpUrl);
                ch.setHttpMethod(chHttpMethod);
                ch.setHttpContentType(chHttpContentType);
                ch.setHttpAuthHeader(chHttpAuthHeader);

                // Scheduling
                ch.setPollIntervalMs(chPollMs);
                ch.setConnectionTimeoutMs(chTimeoutMs);

                ch.setCreatedAt(java.time.LocalDateTime.now());
                ch.setUpdatedAt(java.time.LocalDateTime.now());
                channelRepo.save(ch);
            }

            ra.addFlashAttribute("message", "Partner created: " + created.getPartnerId());
            return "redirect:/ui/partners/" + created.getPartnerId();
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/ui/partners/new";
        }
    }

    @GetMapping("/partners/{partnerId}")
    public String partnerDetail(@PathVariable String partnerId, HttpSession session, Model model) {
        model.addAttribute("activeTab", "partners");
        model.addAttribute("currentUser", currentUser(session));
        return partnerRepo.findByPartnerId(partnerId)
                .map(p -> {
                    model.addAttribute("partner", p);
                    model.addAttribute("channels", channelRepo.findByPartnerId(partnerId));
                    model.addAttribute("recentTransactions",
                            txnRepo.findByPartnerIdOrderByProcessedAtDesc(partnerId, PageRequest.of(0, 10)).getContent());
                    return "partner-detail";
                })
                .orElse("redirect:/ui/partners");
    }

    @GetMapping("/partners/{partnerId}/edit")
    public String editPartnerForm(@PathVariable String partnerId, HttpSession session, Model model) {
        model.addAttribute("activeTab", "partners");
        model.addAttribute("currentUser", currentUser(session));
        model.addAttribute("isEdit", true);
        model.addAttribute("channel", new CommunicationChannel());
        return partnerRepo.findByPartnerId(partnerId)
                .map(p -> { model.addAttribute("partner", p); return "partner-form"; })
                .orElse("redirect:/ui/partners");
    }

    @PostMapping("/partners/{partnerId}/edit")
    public String updatePartner(@PathVariable String partnerId, TradingPartner updates, RedirectAttributes ra) {
        try {
            partnerService.updatePartner(partnerId, updates);
            ra.addFlashAttribute("message", "Partner updated");
            return "redirect:/ui/partners/" + partnerId;
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/ui/partners/" + partnerId + "/edit";
        }
    }

    @PostMapping("/partners/{partnerId}/activate")
    public String activatePartner(@PathVariable String partnerId, RedirectAttributes ra) {
        partnerService.activatePartner(partnerId); ra.addFlashAttribute("message", "Partner activated");
        return "redirect:/ui/partners/" + partnerId;
    }

    @PostMapping("/partners/{partnerId}/suspend")
    public String suspendPartner(@PathVariable String partnerId, RedirectAttributes ra) {
        partnerService.suspendPartner(partnerId); ra.addFlashAttribute("message", "Partner suspended");
        return "redirect:/ui/partners/" + partnerId;
    }

    @PostMapping("/partners/{partnerId}/deactivate")
    public String deactivatePartner(@PathVariable String partnerId, RedirectAttributes ra) {
        partnerService.deactivatePartner(partnerId); ra.addFlashAttribute("message", "Partner deactivated");
        return "redirect:/ui/partners/" + partnerId;
    }

    // ======================== CHANNELS ========================

    @GetMapping("/partners/{partnerId}/channels/new")
    public String newChannelForm(@PathVariable String partnerId, HttpSession session, Model model) {
        model.addAttribute("activeTab", "partners");
        model.addAttribute("currentUser", currentUser(session));
        model.addAttribute("partnerId", partnerId);
        model.addAttribute("channel", new CommunicationChannel());
        model.addAttribute("isEdit", false);
        return "channel-form";
    }

    @PostMapping("/partners/{partnerId}/channels/new")
    public String createChannel(@PathVariable String partnerId, CommunicationChannel channel,
                                HttpSession session, RedirectAttributes ra) {
        ClientUser user = currentUser(session);
        String tid = tenantScope(user);
        channel.setTenantId(tid != null ? tid : "system");
        channel.setPartnerId(partnerId);
        channel.setCreatedAt(java.time.LocalDateTime.now());
        channel.setUpdatedAt(java.time.LocalDateTime.now());
        channelRepo.save(channel);
        ra.addFlashAttribute("message", "Channel created: " + channel.getChannelName());
        return "redirect:/ui/partners/" + partnerId;
    }

    @GetMapping("/channels/{channelId}/edit")
    public String editChannelForm(@PathVariable Long channelId, HttpSession session, Model model) {
        model.addAttribute("activeTab", "partners");
        model.addAttribute("currentUser", currentUser(session));
        return channelRepo.findById(channelId)
                .map(ch -> {
                    model.addAttribute("channel", ch);
                    model.addAttribute("partnerId", ch.getPartnerId());
                    model.addAttribute("isEdit", true);
                    return "channel-form";
                })
                .orElse("redirect:/ui/partners");
    }

    @PostMapping("/channels/{channelId}/edit")
    public String updateChannel(@PathVariable Long channelId, CommunicationChannel updates, RedirectAttributes ra) {
        return channelRepo.findById(channelId)
                .map(existing -> {
                    existing.setChannelName(updates.getChannelName());
                    existing.setProtocol(updates.getProtocol());
                    existing.setDirection(updates.getDirection());
                    existing.setStatus(updates.getStatus());
                    existing.setHost(updates.getHost());
                    existing.setPort(updates.getPort());
                    existing.setUsername(updates.getUsername());
                    if (updates.getPassword() != null && !updates.getPassword().isEmpty()) {
                        existing.setPassword(updates.getPassword());
                    }
                    existing.setRemoteDirectory(updates.getRemoteDirectory());
                    existing.setArchiveDirectory(updates.getArchiveDirectory());
                    existing.setFilePattern(updates.getFilePattern());
                    existing.setPassiveMode(updates.isPassiveMode());
                    existing.setUseTls(updates.isUseTls());
                    existing.setAs2LocalId(updates.getAs2LocalId());
                    existing.setAs2PartnerId(updates.getAs2PartnerId());
                    existing.setAs2Url(updates.getAs2Url());
                    existing.setAs2MdnMode(updates.getAs2MdnMode());
                    existing.setAs2MicAlgorithm(updates.getAs2MicAlgorithm());
                    existing.setAs2EncryptionAlgorithm(updates.getAs2EncryptionAlgorithm());
                    existing.setAs2SignMessages(updates.isAs2SignMessages());
                    existing.setAs2EncryptMessages(updates.isAs2EncryptMessages());
                    existing.setAs2RequestSignedMdn(updates.isAs2RequestSignedMdn());
                    if (updates.getAs2LocalCertificate() != null && !updates.getAs2LocalCertificate().trim().isEmpty()) {
                        existing.setAs2LocalCertificate(updates.getAs2LocalCertificate());
                    }
                    if (updates.getAs2LocalPrivateKey() != null && !updates.getAs2LocalPrivateKey().trim().isEmpty()) {
                        existing.setAs2LocalPrivateKey(updates.getAs2LocalPrivateKey());
                    }
                    if (updates.getAs2PartnerCertificate() != null && !updates.getAs2PartnerCertificate().trim().isEmpty()) {
                        existing.setAs2PartnerCertificate(updates.getAs2PartnerCertificate());
                    }
                    existing.setHttpUrl(updates.getHttpUrl());
                    existing.setHttpMethod(updates.getHttpMethod());
                    existing.setHttpContentType(updates.getHttpContentType());
                    existing.setHttpAuthHeader(updates.getHttpAuthHeader());
                    existing.setHttpCustomHeaders(updates.getHttpCustomHeaders());
                    existing.setPollIntervalMs(updates.getPollIntervalMs());
                    existing.setConnectionTimeoutMs(updates.getConnectionTimeoutMs());
                    existing.setUpdatedAt(java.time.LocalDateTime.now());
                    channelRepo.save(existing);
                    ra.addFlashAttribute("message", "Channel updated");
                    return "redirect:/ui/partners/" + existing.getPartnerId();
                })
                .orElse("redirect:/ui/partners");
    }

    @PostMapping("/channels/{channelId}/test")
    public String testChannel(@PathVariable Long channelId, RedirectAttributes ra) {
        return channelRepo.findById(channelId)
                .map(ch -> {
                    javax.edi.service.channel.ChannelTestService.TestResult result = channelTestService.test(ch);
                    if (result.isSuccess()) {
                        ra.addFlashAttribute("message",
                                "ÃÂ¢ÃÂÃÂ " + ch.getProtocol() + " ÃÂ¢ÃÂÃÂ " + result.getMessage()
                                + " (" + result.getElapsedMs() + " ms)");
                    } else {
                        ra.addFlashAttribute("error",
                                "ÃÂ¢ÃÂÃÂ " + ch.getProtocol() + " ÃÂ¢ÃÂÃÂ " + result.getMessage()
                                + " (" + result.getElapsedMs() + " ms)");
                    }
                    return "redirect:/ui/partners/" + ch.getPartnerId();
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("error", "Channel not found");
                    return "redirect:/ui/partners";
                });
    }

    @PostMapping("/channels/{channelId}/delete")
    public String deleteChannel(@PathVariable Long channelId, RedirectAttributes ra) {
        return channelRepo.findById(channelId)
                .map(ch -> {
                    String partnerId = ch.getPartnerId();
                    channelRepo.delete(ch);
                    ra.addFlashAttribute("message", "Channel deleted: " + ch.getChannelName());
                    return "redirect:/ui/partners/" + partnerId;
                })
                .orElse("redirect:/ui/partners");
    }

    // ======================== USERS ========================

    @GetMapping("/users")
    public String users(HttpSession session, Model model) {
        ClientUser user = currentUser(session);
        String tid = tenantScope(user);
        model.addAttribute("activeTab", "users");
        model.addAttribute("currentUser", user);

        List<ClientUser> users;
        if (tid == null) {
            // Super-admin sees all users
            users = userRepo.findAll();
        } else {
            users = userRepo.findByTenantId(tid);
        }
        model.addAttribute("users", users);
        return "users";
    }

    @GetMapping("/users/invite")
    public String inviteUserForm(HttpSession session, Model model) {
        model.addAttribute("activeTab", "users");
        model.addAttribute("currentUser", currentUser(session));
        return "user-invite";
    }

    @PostMapping("/users/invite")
    public String inviteUser(@RequestParam String fullName, @RequestParam String email,
                             @RequestParam String password, @RequestParam String role,
                             HttpSession session, RedirectAttributes ra) {
        try {
            ClientUser user = currentUser(session);
            String tid = tenantScope(user);
            ClientUser.UserRole userRole = ClientUser.UserRole.valueOf(role);
            authService.createUser(tid != null ? tid : "system", email, fullName, password, userRole);
            ra.addFlashAttribute("message", "User created: " + email);
            return "redirect:/ui/users";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/ui/users/invite";
        }
    }

    @PostMapping("/users/{userId}/disable")
    public String disableUser(@PathVariable Long userId, RedirectAttributes ra) {
        userRepo.findById(userId).ifPresent(u -> {
            u.setStatus(ClientUser.UserStatus.DISABLED);
            u.setUpdatedAt(java.time.LocalDateTime.now());
            userRepo.save(u);
        });
        ra.addFlashAttribute("message", "User disabled");
        return "redirect:/ui/users";
    }

    @PostMapping("/users/{userId}/enable")
    public String enableUser(@PathVariable Long userId, RedirectAttributes ra) {
        userRepo.findById(userId).ifPresent(u -> {
            u.setStatus(ClientUser.UserStatus.ACTIVE);
            u.setUpdatedAt(java.time.LocalDateTime.now());
            userRepo.save(u);
        });
        ra.addFlashAttribute("message", "User enabled");
        return "redirect:/ui/users";
    }

    // ======================== DELFOR D96A VIEWER (DEMO) ========================

//    @GetMapping("/delfor-viewer")
//    public String delforViewer(HttpSession session, Model model) {
//        ClientUser user = currentUser(session);
//        model.addAttribute("activeTab", "delfor-viewer");
//        model.addAttribute("currentUser", user);
//
//        try {
//            // Read the embedded DELFORD96A.edi file
//            ClassPathResource res = new ClassPathResource("static/data/DELFORD96A.edi");
//            String rawEdi = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
//            model.addAttribute("rawEdi", rawEdi);
//
//            // Parse using the JAEB EDIFACT unmarshaller
//            EDIFACTUnmarshalResult<DelforD96A> result =
//                    EDIFACTUnmarshaller.unmarshal(DelforD96A.class, new StringReader(rawEdi));
//
//            model.addAttribute("parsed", result.isParsed());
//            model.addAttribute("segmentCount", result.getSegmentCount());
//            model.addAttribute("parseTimeMs", result.getParseTimeMillis());
//            model.addAttribute("senderIdentification", result.getSenderIdentification());
//            model.addAttribute("receiverIdentification", result.getReceiverIdentification());
//            model.addAttribute("interchangeReference", result.getInterchangeReference());
//            model.addAttribute("messageType", result.getMessageType());
//            model.addAttribute("messageVersion", result.getMessageVersion());
//            model.addAttribute("messageRelease", result.getMessageRelease());
//
//            DelforD96A delfor = result.getData();
//            if (delfor != null) {
//                model.addAttribute("delfor", delfor);
//
//                // Convert to JSON for the JSON tab
//                com.fasterxml.jackson.databind.ObjectMapper mapper =
//                        new com.fasterxml.jackson.databind.ObjectMapper();
//                mapper.setSerializationInclusion(
//                        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
//                String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(delfor);
//                model.addAttribute("jsonOutput", json);
//            }
//        } catch (Exception e) {
//            model.addAttribute("parsed", false);
//            model.addAttribute("parseError", e.getMessage());
//        }
//
//        return "delfor-viewer";
//    }

    // ======================== X12 850 PURCHASE ORDER VIEWER (DEMO) ========================

    @GetMapping("/po-viewer")
    public String poViewer(HttpSession session, Model model) {
        ClientUser user = currentUser(session);
        model.addAttribute("activeTab", "po-viewer");
        model.addAttribute("currentUser", user);

        try {
            ClassPathResource res = new ClassPathResource("static/data/850.edi");
            String rawEdi = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            model.addAttribute("rawEdi", rawEdi);

            EDIUnmarshalResult<PurchaseOrder> result =
                    EDIUnmarshaller.unmarshalResult(PurchaseOrder.class, new StringReader(rawEdi));

            model.addAttribute("parsed", result.isParsed());
            model.addAttribute("segmentCount", result.getSegmentCount());
            model.addAttribute("parseTimeMs", result.getParseTimeMillis());

            PurchaseOrder po = result.getData();
            if (po != null) {
                model.addAttribute("po", po);

                // Convert to JSON for the JSON tab
                com.fasterxml.jackson.databind.ObjectMapper mapper =
                        new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.setSerializationInclusion(
                        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
                mapper.configure(
                        com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
                String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(po);
                model.addAttribute("jsonOutput", json);
            }
        } catch (Exception e) {
            model.addAttribute("parsed", false);
            model.addAttribute("parseError", e.getMessage());
        }

        return "po-viewer";
    }

    // ======================== HELPERS ========================

    private ClientUser currentUser(HttpSession session) {
        return (ClientUser) session.getAttribute("user");
    }

    /** Returns tenantId for scoping, or null if user is super-admin (sees all). */
    private String tenantScope(ClientUser user) {
        if (user == null) return null;
        return user.isSuperAdmin() ? null : user.getTenantId();
    }

    private List<Integer> buildPageNumbers(int current, int total) {
        List<Integer> pages = new ArrayList<>();
        if (total <= 7) {
            for (int i = 0; i < total; i++) pages.add(i);
        } else {
            int start = Math.max(0, current - 3);
            int end = Math.min(total - 1, current + 3);
            if (start > 0) pages.add(0);
            for (int i = start; i <= end; i++) pages.add(i);
            if (end < total - 1) pages.add(total - 1);
        }
        return pages;
    }
}

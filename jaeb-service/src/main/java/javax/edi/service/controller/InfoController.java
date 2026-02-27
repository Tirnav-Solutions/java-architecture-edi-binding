package javax.edi.service.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.edi.service.dto.ApiResponse;
import javax.edi.service.registry.EDIModelRegistry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * GET /api/edi/types      -- List all supported transaction types
 * GET /api/edi/health     -- Service health check
 * GET /api/edi/info       -- Service info and API summary
 */
@RestController
@RequestMapping("/api/edi")
@Tag(name = "Info", description = "Service info, supported types, and health")
public class InfoController {

    @Autowired
    private EDIModelRegistry registry;

    @Value("${edi.security.api-key-enabled:false}")
    private boolean apiKeyEnabled;

    @Value("${edi.security.rate-limit-enabled:false}")
    private boolean rateLimitEnabled;

    @Value("${edi.security.rate-limit-per-minute:100}")
    private int rateLimitPerMinute;

    @Value("${edi.audit.logging-enabled:true}")
    private boolean auditLoggingEnabled;

    @Value("${edi.sftp.polling-enabled:false}")
    private boolean sftpPollingEnabled;

    @Operation(summary = "List supported EDI transaction types",
               description = "Returns all registered transaction types that can be "
                           + "used with the parse, generate, validate, schema, sample, and doc endpoints.")
    @GetMapping(value = "/types", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> types() {
        Map<String, String> supportedTypes = registry.getSupportedTypes();
        return ResponseEntity.ok(ApiResponse.ok(supportedTypes,
                "Supported transaction types (" + supportedTypes.size() + ")"));
    }

    @Operation(summary = "Service health check")
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("service", "JAEB EDI Middleware");
        data.put("registeredTypes", registry.getSupportedTypes().size());
        return ResponseEntity.ok(ApiResponse.ok(data, "Service is healthy"));
    }

    @Operation(summary = "Service information and API summary",
               description = "Returns general information about the service and "
                           + "available API endpoints.")
    @GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> info() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "JAEB EDI-as-a-Service Platform");
        data.put("version", "0.0.1-SNAPSHOT");
        data.put("description", "Full EDI-as-a-Service platform with parsing, generation, "
                + "validation, 997 acknowledgement, partner onboarding, SFTP polling, "
                + "transaction logging, and dashboard.");

        Map<String, String> endpoints = new LinkedHashMap<>();

        // EDI Operations
        endpoints.put("POST /api/edi/parse",         "Parse raw EDI text into JSON");
        endpoints.put("POST /api/edi/generate",      "Generate raw EDI from JSON payload");
        endpoints.put("POST /api/edi/validate",      "Validate raw EDI or JSON payload");
        endpoints.put("POST /api/edi/acknowledge",   "Parse EDI + auto-generate 997 (logged to DB)");
        endpoints.put("POST /api/edi/convert",       "Convert EDI between delimiter formats");
        endpoints.put("POST /api/edi/batch/parse",   "Batch parse multiple EDI documents");
        endpoints.put("POST /api/edi/compare",       "Compare two EDI documents segment-by-segment");

        // Schema & Docs
        endpoints.put("GET  /api/edi/schema/{type}", "JSON Schema for a transaction type");
        endpoints.put("GET  /api/edi/sample/{type}", "Sample JSON for a transaction type");
        endpoints.put("GET  /api/edi/doc/{type}",    "Implementation Guide (TEXT/MARKDOWN/HTML)");
        endpoints.put("GET  /api/edi/doc/{type}/view", "View guide in browser (shareable URL)");

        // Trading Partners
        endpoints.put("GET  /api/partners",          "List all trading partners");
        endpoints.put("GET  /api/partners/active",   "List active trading partners");
        endpoints.put("GET  /api/partners/{id}",     "Get partner by ID");
        endpoints.put("POST /api/partners",          "Onboard a new trading partner");
        endpoints.put("PUT  /api/partners/{id}",     "Update partner configuration");
        endpoints.put("POST /api/partners/{id}/activate",   "Activate partner");
        endpoints.put("POST /api/partners/{id}/suspend",    "Suspend partner");
        endpoints.put("POST /api/partners/{id}/deactivate", "Deactivate partner");

        // Transactions & Dashboard
        endpoints.put("GET  /api/transactions/dashboard",   "Transaction summary dashboard");
        endpoints.put("GET  /api/transactions",             "List all transactions (paginated)");
        endpoints.put("GET  /api/transactions/{txnId}",     "Get transaction by tracking ID");
        endpoints.put("GET  /api/transactions/partner/{id}","Transactions by partner");
        endpoints.put("GET  /api/transactions/type/{type}", "Transactions by type");
        endpoints.put("GET  /api/transactions/status/{s}",  "Transactions by status");
        endpoints.put("GET  /api/transactions/date-range",  "Transactions in date range");
        endpoints.put("GET  /api/transactions/control-number/{n}", "Search by ISA control number");

        // SFTP
        endpoints.put("POST /api/transactions/sftp/poll",       "Trigger SFTP poll for all partners");
        endpoints.put("POST /api/transactions/sftp/poll/{id}",  "Trigger SFTP poll for one partner");

        // Info
        endpoints.put("GET  /api/edi/types",         "List supported transaction types");
        endpoints.put("GET  /api/edi/health",        "Service health check");
        endpoints.put("GET  /api/edi/info",          "This endpoint");

        // Web UI
        endpoints.put("GET  /ui/dashboard",          "Web UI — Dashboard");
        endpoints.put("GET  /ui/transactions",       "Web UI — Transaction log with filters");
        endpoints.put("GET  /ui/transactions/{id}",  "Web UI — Transaction detail viewer");
        endpoints.put("GET  /ui/partners",           "Web UI — Partner management");
        endpoints.put("GET  /ui/partners/new",       "Web UI — Onboard new partner");
        endpoints.put("GET  /ui/partners/{id}",      "Web UI — Partner detail");
        data.put("endpoints", endpoints);

        data.put("supportedTypes", registry.getSupportedTypes());

        Map<String, Object> security = new LinkedHashMap<>();
        security.put("apiKeyAuthentication", apiKeyEnabled ? "ENABLED" : "DISABLED");
        security.put("rateLimiting", rateLimitEnabled ? "ENABLED (" + rateLimitPerMinute + " req/min)" : "DISABLED");
        security.put("auditLogging", auditLoggingEnabled ? "ENABLED" : "DISABLED");
        security.put("sftpPolling", sftpPollingEnabled ? "ENABLED" : "DISABLED");
        security.put("database", "H2 (file-based)");
        security.put("cors", "ENABLED (all origins)");
        data.put("features", security);

        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}

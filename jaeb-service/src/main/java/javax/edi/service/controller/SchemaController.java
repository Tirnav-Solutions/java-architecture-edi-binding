package javax.edi.service.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.edi.bind.schema.EDIDocGenerator;
import javax.edi.bind.schema.EDISampleGenerator;
import javax.edi.bind.schema.EDISchemaGenerator;
import javax.edi.service.dto.ApiResponse;
import javax.edi.service.registry.EDIModelRegistry;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * GET /api/edi/schema/{type}   -- JSON Schema
 * GET /api/edi/sample/{type}   -- Sample JSON
 * GET /api/edi/doc/{type}      -- Implementation Guide (text or markdown)
 * 
 * All endpoints accept a transaction type key (e.g. "850", "PurchaseOrder").
 */
@RestController
@RequestMapping("/api/edi")
@Tag(name = "Schema & Docs", description = "Generate JSON Schema, sample JSON, and implementation guides")
public class SchemaController {

    private static final Logger LOG = LoggerFactory.getLogger(SchemaController.class);

    @Autowired
    private EDIModelRegistry registry;

    // ==================== JSON Schema ====================

    @Operation(summary = "Generate JSON Schema for a transaction type",
               description = "Produces a JSON Schema (Draft-07) from the EDI model class "
                           + "annotations. Useful for API documentation and payload validation.")
    @GetMapping(value = "/schema/{type}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> schema(
            @Parameter(description = "Transaction type key (e.g. '850', 'PurchaseOrder')")
            @PathVariable String type,
            @Parameter(description = "Pretty-print the schema output")
            @RequestParam(defaultValue = "true") boolean pretty) {

        Class<?> modelClass = registry.resolve(type);
        if (modelClass == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unknown type: '" + type
                            + "'. Supported: " + registry.getSupportedTypes().keySet()));
        }

        try {
            String schema = EDISchemaGenerator.generateSchema(modelClass, pretty);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("transactionType", type);
            data.put("modelClass", modelClass.getName());
            data.put("schema", schema);

            return ResponseEntity.ok(ApiResponse.ok(data, "JSON Schema generated"));
        } catch (Exception e) {
            LOG.error("Schema generation failed for {}: {}", type, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Schema generation failed: " + e.getMessage()));
        }
    }

    // ==================== Sample JSON ====================

    @Operation(summary = "Generate sample JSON for a transaction type",
               description = "Produces a sample/template JSON with placeholder values "
                           + "based on field types and validation annotations. "
                           + "Useful for onboarding and testing.")
    @GetMapping(value = "/sample/{type}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> sample(
            @Parameter(description = "Transaction type key (e.g. '850', 'PurchaseOrder')")
            @PathVariable String type) {

        Class<?> modelClass = registry.resolve(type);
        if (modelClass == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unknown type: '" + type
                            + "'. Supported: " + registry.getSupportedTypes().keySet()));
        }

        try {
            String sampleJson = EDISampleGenerator.generateSample(modelClass);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("transactionType", type);
            data.put("modelClass", modelClass.getName());
            data.put("sample", sampleJson);

            return ResponseEntity.ok(ApiResponse.ok(data, "Sample JSON generated"));
        } catch (Exception e) {
            LOG.error("Sample generation failed for {}: {}", type, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Sample generation failed: " + e.getMessage()));
        }
    }

    // ==================== Implementation Guide ====================

    @Operation(summary = "Generate EDI Implementation Guide",
               description = "Produces a human-readable implementation guide listing all "
                           + "segments, elements, positions, data types, and constraints. "
                           + "Supports TEXT, MARKDOWN, and HTML output formats. "
                           + "Response includes a viewUrl for viewing the document in a browser.")
    @GetMapping(value = "/doc/{type}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> doc(
            @Parameter(description = "Transaction type key (e.g. '850', 'PurchaseOrder')")
            @PathVariable String type,
            @Parameter(description = "Output format: TEXT, MARKDOWN, or HTML")
            @RequestParam(defaultValue = "TEXT") String format,
            HttpServletRequest request) {

        Class<?> modelClass = registry.resolve(type);
        if (modelClass == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unknown type: '" + type
                            + "'. Supported: " + registry.getSupportedTypes().keySet()));
        }

        try {
            EDIDocGenerator.Format docFormat;
            try {
                docFormat = EDIDocGenerator.Format.valueOf(format.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid format: '" + format
                                + "'. Supported: TEXT, MARKDOWN, HTML"));
            }

            // Build the dynamic view URL
            String viewUrl = buildViewUrl(request, type);

            String doc;
            if (docFormat == EDIDocGenerator.Format.HTML) {
                doc = EDIDocGenerator.generateHtml(modelClass, viewUrl);
            } else {
                doc = EDIDocGenerator.generate(modelClass, docFormat);
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("transactionType", type);
            data.put("modelClass", modelClass.getName());
            data.put("format", docFormat.name());
            data.put("viewUrl", viewUrl);
            data.put("document", doc);

            return ResponseEntity.ok(ApiResponse.ok(data, "Implementation guide generated"));
        } catch (Exception e) {
            LOG.error("Doc generation failed for {}: {}", type, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Doc generation failed: " + e.getMessage()));
        }
    }

    // ==================== Browser-viewable HTML document ====================

    @Operation(summary = "View EDI Implementation Guide in browser",
               description = "Serves a self-contained, styled HTML page that renders the "
                           + "EDI implementation guide directly in the browser. "
                           + "Share this URL with trading partners or developers.")
    @GetMapping(value = "/doc/{type}/view", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> docView(
            @Parameter(description = "Transaction type key (e.g. '850', 'PurchaseOrder')")
            @PathVariable String type,
            HttpServletRequest request) {

        Class<?> modelClass = registry.resolve(type);
        if (modelClass == null) {
            return ResponseEntity.badRequest()
                    .body("<html><body><h1>Unknown type: " + type + "</h1>"
                            + "<p>Supported: " + registry.getSupportedTypes().keySet() + "</p></body></html>");
        }

        String viewUrl = buildViewUrl(request, type);
        String html = EDIDocGenerator.generateHtml(modelClass, viewUrl);
        return ResponseEntity.ok(html);
    }

    // ==================== Raw outputs (text/plain) ====================

    @Operation(summary = "Get raw JSON Schema as text",
               description = "Returns the raw JSON Schema string directly (no wrapper).")
    @GetMapping(value = "/schema/{type}/raw", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> schemaRaw(@PathVariable String type) {
        Class<?> modelClass = registry.resolve(type);
        if (modelClass == null) {
            return ResponseEntity.badRequest().body("Unknown type: " + type);
        }
        return ResponseEntity.ok(EDISchemaGenerator.generateSchema(modelClass, true));
    }

    @Operation(summary = "Get raw sample JSON as text",
               description = "Returns the raw sample JSON string directly (no wrapper).")
    @GetMapping(value = "/sample/{type}/raw", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> sampleRaw(@PathVariable String type) {
        Class<?> modelClass = registry.resolve(type);
        if (modelClass == null) {
            return ResponseEntity.badRequest().body("Unknown type: " + type);
        }
        return ResponseEntity.ok(EDISampleGenerator.generateSample(modelClass));
    }

    @Operation(summary = "Get raw implementation guide as text",
               description = "Returns the raw implementation guide directly (no JSON wrapper).")
    @GetMapping(value = "/doc/{type}/raw", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> docRaw(
            @PathVariable String type,
            @RequestParam(defaultValue = "TEXT") String format) {
        Class<?> modelClass = registry.resolve(type);
        if (modelClass == null) {
            return ResponseEntity.badRequest().body("Unknown type: " + type);
        }
        EDIDocGenerator.Format docFormat;
        try {
            docFormat = EDIDocGenerator.Format.valueOf(format.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid format: " + format);
        }
        return ResponseEntity.ok(EDIDocGenerator.generate(modelClass, docFormat));
    }

    // ==================== URL Builder ====================

    /**
     * Builds a dynamic view URL from the incoming request.
     * Handles proxy headers (X-Forwarded-*) for services behind a load balancer.
     */
    private String buildViewUrl(HttpServletRequest request, String type) {
        // Check for reverse proxy / load balancer headers
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null || scheme.isEmpty()) {
            scheme = request.getScheme();
        }

        String host = request.getHeader("X-Forwarded-Host");
        if (host == null || host.isEmpty()) {
            host = request.getHeader("Host");
        }
        if (host == null || host.isEmpty()) {
            host = request.getServerName();
            int port = request.getServerPort();
            if (("http".equals(scheme) && port != 80) || ("https".equals(scheme) && port != 443)) {
                host = host + ":" + port;
            }
        }

        return scheme + "://" + host + "/api/edi/doc/" + type + "/view";
    }
}

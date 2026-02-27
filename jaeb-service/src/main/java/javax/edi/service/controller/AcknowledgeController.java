package javax.edi.service.controller;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.edi.bind.EDIUnmarshaller;
import javax.edi.bind.EDIUnmarshalResult;
import javax.edi.bind.util.EDI997Generator;
import javax.edi.configuration.EDIMessageConfiguration;
import javax.edi.service.dto.ApiResponse;
import javax.edi.service.dto.ParseRequest;
import javax.edi.service.entity.EDITransactionLog;
import javax.edi.service.registry.EDIModelRegistry;
import javax.edi.service.service.TransactionLoggingService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * POST /api/edi/acknowledge
 * 
 * Accepts raw EDI content, parses it, and automatically generates
 * an EDI 997 Functional Acknowledgement in response.
 * 
 * This is the core "middleware" use case: receive any X12 transaction,
 * parse it, and produce the 997 acknowledgement in one call.
 */
@RestController
@RequestMapping("/api/edi")
@Tag(name = "Acknowledge", description = "Parse EDI and generate 997 Functional Acknowledgement")
public class AcknowledgeController {

    private static final Logger LOG = LoggerFactory.getLogger(AcknowledgeController.class);

    @Autowired
    private EDIModelRegistry registry;

    @Autowired
    private TransactionLoggingService txnLoggingService;

    @Operation(summary = "Parse EDI and generate 997 acknowledgement",
               description = "Accepts raw EDI text and a transaction type, parses it, "
                           + "and generates a compliant EDI 997 Functional Acknowledgement. "
                           + "The 997 swaps sender/receiver from the original ISA/GS envelopes, "
                           + "sets AK9 acknowledge code based on validation status, and includes "
                           + "AK2/AK5 pairs for each transaction set found. "
                           + "Returns both the parsed data and the generated 997.")
    @PostMapping(value = "/acknowledge", consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> acknowledge(@RequestBody ParseRequest request) {

        // --- Validate input ---
        if (request.getEdiContent() == null || request.getEdiContent().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("ediContent is required"));
        }
        if (request.getTransactionType() == null || request.getTransactionType().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("transactionType is required (e.g. '850', 'PurchaseOrder')"));
        }

        Class<?> modelClass = registry.resolve(request.getTransactionType());
        if (modelClass == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unknown transactionType: '" + request.getTransactionType()
                            + "'. Supported types: " + registry.getSupportedTypes().keySet()));
        }

        try {
            // Step 1: Parse the incoming EDI
            StringReader reader = new StringReader(request.getEdiContent());
            EDIUnmarshalResult<?> parseResult;

            if (hasCustomDelimiters(request)) {
                char elemDelim = request.getElementDelimiter() != null ? request.getElementDelimiter() : '*';
                char segDelim = request.getSegmentDelimiter() != null ? request.getSegmentDelimiter() : '~';
                char compDelim = request.getComponentDelimiter() != null ? request.getComponentDelimiter() : '>';
                EDIMessageConfiguration config = new EDIMessageConfiguration(elemDelim, compDelim, segDelim);
                parseResult = EDIUnmarshaller.unmarshalResult(modelClass, reader, config);
            } else {
                parseResult = EDIUnmarshaller.unmarshalResult(modelClass, reader);
            }

            // Step 2: Generate 997 from the parse result
            EDI997Generator.Result ackResult = EDI997Generator.generate(parseResult);

            // Step 2.5: Log to database
            EDITransactionLog txnLog = txnLoggingService.logInboundTransaction(
                    request.getEdiContent(), request.getTransactionType(),
                    parseResult, ackResult, null,
                    EDITransactionLog.Source.API, null);

            // Step 3: Build response
            Map<String, Object> data = new LinkedHashMap<>();

            // Parse info
            Map<String, Object> parseInfo = new LinkedHashMap<>();
            parseInfo.put("parsed", parseResult.isParsed());
            parseInfo.put("valid", parseResult.isValid());
            parseInfo.put("segmentCount", parseResult.getSegmentCount());
            parseInfo.put("parseTimeMs", parseResult.getParseTimeMillis());
            if (parseResult.getParseError() != null) {
                parseInfo.put("parseError", parseResult.getParseError());
            }
            if (parseResult.hasErrors()) {
                parseInfo.put("validationErrorCount", parseResult.getErrorCount());
                parseInfo.put("validationErrors", parseResult.getErrorSummary());
            }
            data.put("parseResult", parseInfo);

            // Parsed data
            data.put("parsedData", parseResult.getData());

            // 997 acknowledgement
            Map<String, Object> ackInfo = new LinkedHashMap<>();
            ackInfo.put("success", ackResult.isSuccess());
            ackInfo.put("acknowledgeCode", ackResult.getAcknowledgeCode());
            ackInfo.put("controlNumber", ackResult.getControlNumber());
            ackInfo.put("segmentCount", ackResult.getSegmentCount());
            if (ackResult.getError() != null) {
                ackInfo.put("error", ackResult.getError());
            }
            if (ackResult.isSuccess()) {
                ackInfo.put("edi997Output", ackResult.getEdiOutput());
            }
            data.put("acknowledgement", ackInfo);

            // Metadata
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("transactionId", txnLog.getTransactionId());
            metadata.put("transactionType", request.getTransactionType());
            metadata.put("modelClass", modelClass.getName());
            metadata.put("ackGenerated", ackResult.isSuccess());

            String message;
            if (ackResult.isSuccess()) {
                message = "EDI parsed and 997 acknowledgement generated (AK9="
                        + ackResult.getAcknowledgeCode() + ")";
            } else {
                message = "EDI parsed but 997 generation failed: " + ackResult.getError();
            }

            ApiResponse response = ApiResponse.ok(data, metadata);
            response.setMessage(message);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LOG.error("Acknowledge failed for type {}: {}", request.getTransactionType(), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Acknowledge failed: " + e.getMessage()));
        }
    }

    private boolean hasCustomDelimiters(ParseRequest req) {
        return req.getElementDelimiter() != null
            || req.getSegmentDelimiter() != null
            || req.getComponentDelimiter() != null;
    }
}

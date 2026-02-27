package javax.edi.service.controller;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.edi.bind.EDIUnmarshaller;
import javax.edi.bind.EDIUnmarshalResult;
import javax.edi.bind.util.EDI997Generator;
import javax.edi.bind.util.EDIValidationError;
import javax.edi.configuration.EDIMessageConfiguration;
import javax.edi.service.dto.ApiResponse;
import javax.edi.service.dto.BatchParseRequest;
import javax.edi.service.registry.EDIModelRegistry;

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
 * POST /api/edi/batch/parse
 * 
 * Parses multiple EDI documents in a single API call.
 * Returns individual results for each document along with a 997 acknowledgement
 * for each one. Supports continue-on-error mode.
 */
@RestController
@RequestMapping("/api/edi")
@Tag(name = "Batch", description = "Batch processing of multiple EDI documents")
public class BatchController {

    private static final Logger LOG = LoggerFactory.getLogger(BatchController.class);

    @Autowired
    private EDIModelRegistry registry;

    @Operation(summary = "Batch parse multiple EDI documents",
               description = "Parses multiple EDI documents in a single request. "
                           + "Returns individual parse results and 997 acknowledgements "
                           + "for each document. Use continueOnError=true (default) to "
                           + "process remaining documents even if one fails.")
    @PostMapping(value = "/batch/parse", consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> batchParse(@RequestBody BatchParseRequest request) {

        // --- Validate input ---
        if (request.getTransactionType() == null || request.getTransactionType().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("transactionType is required"));
        }
        if (request.getEdiContents() == null || request.getEdiContents().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("ediContents list is required and must not be empty"));
        }

        Class<?> modelClass = registry.resolve(request.getTransactionType());
        if (modelClass == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unknown transactionType: '" + request.getTransactionType()
                            + "'. Supported: " + registry.getSupportedTypes().keySet()));
        }

        long batchStart = System.currentTimeMillis();
        List<Map<String, Object>> results = new ArrayList<>();
        int totalParsed = 0;
        int totalFailed = 0;
        int totalValid = 0;
        int totalSegments = 0;

        for (int i = 0; i < request.getEdiContents().size(); i++) {
            String ediContent = request.getEdiContents().get(i);
            Map<String, Object> itemResult = new LinkedHashMap<>();
            itemResult.put("index", i);

            if (ediContent == null || ediContent.trim().isEmpty()) {
                itemResult.put("status", "SKIPPED");
                itemResult.put("error", "Empty EDI content at index " + i);
                results.add(itemResult);
                totalFailed++;
                continue;
            }

            try {
                StringReader reader = new StringReader(ediContent);
                EDIUnmarshalResult<?> parseResult;

                if (hasCustomDelimiters(request)) {
                    char elemDelim = request.getElementDelimiter() != null ? request.getElementDelimiter() : '*';
                    char segDelim  = request.getSegmentDelimiter() != null ? request.getSegmentDelimiter() : '~';
                    char compDelim = request.getComponentDelimiter() != null ? request.getComponentDelimiter() : '>';
                    EDIMessageConfiguration config = new EDIMessageConfiguration(elemDelim, compDelim, segDelim);
                    parseResult = EDIUnmarshaller.unmarshalResult(modelClass, reader, config);
                } else {
                    parseResult = EDIUnmarshaller.unmarshalResult(modelClass, reader);
                }

                itemResult.put("status", parseResult.isParsed() ? "PARSED" : "FAILED");
                itemResult.put("parsed", parseResult.isParsed());
                itemResult.put("valid", parseResult.isValid());
                itemResult.put("segmentCount", parseResult.getSegmentCount());
                itemResult.put("parseTimeMs", parseResult.getParseTimeMillis());

                if (parseResult.getParseError() != null) {
                    itemResult.put("parseError", parseResult.getParseError());
                }
                if (parseResult.hasErrors()) {
                    List<String> errors = new ArrayList<>();
                    for (EDIValidationError err : parseResult.getErrors()) {
                        errors.add(err.toString());
                    }
                    itemResult.put("validationErrors", errors);
                }

                itemResult.put("data", parseResult.getData());

                // Generate 997 for each document
                EDI997Generator.Result ackResult = EDI997Generator.generate(parseResult);
                if (ackResult.isSuccess()) {
                    Map<String, Object> ack = new LinkedHashMap<>();
                    ack.put("acknowledgeCode", ackResult.getAcknowledgeCode());
                    ack.put("controlNumber", ackResult.getControlNumber());
                    ack.put("edi997Output", ackResult.getEdiOutput());
                    itemResult.put("acknowledgement", ack);
                }

                if (parseResult.isParsed()) totalParsed++;
                else totalFailed++;
                if (parseResult.isValid()) totalValid++;
                totalSegments += parseResult.getSegmentCount();

            } catch (Exception e) {
                itemResult.put("status", "ERROR");
                itemResult.put("error", e.getMessage());
                totalFailed++;

                if (!request.isContinueOnError()) {
                    results.add(itemResult);
                    break;
                }
            }

            results.add(itemResult);
        }

        long batchTimeMs = System.currentTimeMillis() - batchStart;

        // Build summary
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("results", results);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalDocuments", request.getEdiContents().size());
        summary.put("processed", results.size());
        summary.put("parsed", totalParsed);
        summary.put("failed", totalFailed);
        summary.put("valid", totalValid);
        summary.put("totalSegments", totalSegments);
        summary.put("batchTimeMs", batchTimeMs);
        data.put("summary", summary);

        ApiResponse response = ApiResponse.ok(data);
        response.setMessage("Batch parse complete: " + totalParsed + " parsed, "
                + totalFailed + " failed, " + totalValid + " valid out of "
                + request.getEdiContents().size() + " documents (" + batchTimeMs + "ms)");
        return ResponseEntity.ok(response);
    }

    private boolean hasCustomDelimiters(BatchParseRequest req) {
        return req.getElementDelimiter() != null
            || req.getSegmentDelimiter() != null
            || req.getComponentDelimiter() != null;
    }
}

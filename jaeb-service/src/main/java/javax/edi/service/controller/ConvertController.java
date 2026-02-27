package javax.edi.service.controller;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.edi.bind.EDIMarshaller;
import javax.edi.bind.EDIUnmarshaller;
import javax.edi.bind.EDIUnmarshalResult;
import javax.edi.configuration.EDIMessageConfiguration;
import javax.edi.service.dto.ApiResponse;
import javax.edi.service.dto.ConvertRequest;
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
 * POST /api/edi/convert
 * 
 * Converts an EDI document from one delimiter format to another.
 * Useful when trading partners use different delimiter conventions.
 * 
 * e.g., Convert from pipe-delimited (|) to asterisk-delimited (*),
 * or from tilde (~) segment terminator to newline-based.
 */
@RestController
@RequestMapping("/api/edi")
@Tag(name = "Convert", description = "Convert EDI between delimiter formats")
public class ConvertController {

    private static final Logger LOG = LoggerFactory.getLogger(ConvertController.class);

    @Autowired
    private EDIModelRegistry registry;

    @Operation(summary = "Convert EDI delimiter format",
               description = "Parses an EDI document using source delimiters, then "
                           + "re-serializes it with target delimiters. Useful when "
                           + "trading partners use different delimiter conventions.")
    @PostMapping(value = "/convert", consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> convert(@RequestBody ConvertRequest request) {

        if (request.getEdiContent() == null || request.getEdiContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("ediContent is required"));
        }
        if (request.getTransactionType() == null || request.getTransactionType().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("transactionType is required"));
        }

        Class<?> modelClass = registry.resolve(request.getTransactionType());
        if (modelClass == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unknown transactionType: '" + request.getTransactionType()
                            + "'. Supported: " + registry.getSupportedTypes().keySet()));
        }

        try {
            // Step 1: Parse with source delimiters
            char srcElem = request.getSourceElementDelimiter() != null ? request.getSourceElementDelimiter() : '*';
            char srcSeg  = request.getSourceSegmentDelimiter() != null ? request.getSourceSegmentDelimiter() : '~';
            char srcComp = request.getSourceComponentDelimiter() != null ? request.getSourceComponentDelimiter() : '>';

            EDIMessageConfiguration srcConfig = new EDIMessageConfiguration(srcElem, srcComp, srcSeg);
            StringReader reader = new StringReader(request.getEdiContent());
            Object parsedObject = EDIUnmarshaller.unmarshal(modelClass, reader, srcConfig);

            // Step 2: Marshal with target delimiters
            char tgtElem = request.getTargetElementDelimiter() != null ? request.getTargetElementDelimiter() : '*';
            char tgtSeg  = request.getTargetSegmentDelimiter() != null ? request.getTargetSegmentDelimiter() : '~';
            char tgtComp = request.getTargetComponentDelimiter() != null ? request.getTargetComponentDelimiter() : '>';

            EDIMessageConfiguration tgtConfig = new EDIMessageConfiguration(tgtElem, tgtComp, tgtSeg);
            StringWriter writer = new StringWriter();
            EDIMarshaller.marshal(parsedObject, writer, tgtConfig);

            String convertedEdi = writer.toString();

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("convertedEdi", convertedEdi);

            Map<String, String> sourceDelimiters = new LinkedHashMap<>();
            sourceDelimiters.put("element", String.valueOf(srcElem));
            sourceDelimiters.put("segment", String.valueOf(srcSeg));
            sourceDelimiters.put("component", String.valueOf(srcComp));
            data.put("sourceDelimiters", sourceDelimiters);

            Map<String, String> targetDelimiters = new LinkedHashMap<>();
            targetDelimiters.put("element", String.valueOf(tgtElem));
            targetDelimiters.put("segment", String.valueOf(tgtSeg));
            targetDelimiters.put("component", String.valueOf(tgtComp));
            data.put("targetDelimiters", targetDelimiters);

            ApiResponse response = ApiResponse.ok(data);
            response.setMessage("EDI converted successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LOG.error("Convert failed for type {}: {}", request.getTransactionType(), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Conversion failed: " + e.getMessage()));
        }
    }
}

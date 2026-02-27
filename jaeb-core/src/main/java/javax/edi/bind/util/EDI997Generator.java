package javax.edi.bind.util;

import java.io.StringWriter;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.edi.bind.EDIMarshaller;
import javax.edi.bind.EDIMarshalResult;
import javax.edi.bind.EDIUnmarshalResult;
import javax.edi.bind.annotations.EDIMessage;
import javax.edi.bind.annotations.EDISegment;
import javax.edi.bind.annotations.EDISegmentGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates an EDI 997 Functional Acknowledgement from a parsed EDI message.
 * 
 * After unmarshalling any X12 transaction set (850, 810, 856, etc.), call
 * this utility to produce a compliant 997 response that acknowledges receipt
 * and reports validation status.
 * 
 * The generator is model-agnostic -- it uses reflection to extract ISA/GS
 * envelope data from any @EDIMessage class that follows the standard X12
 * envelope pattern (envelopeHeader, groupEnvelopeHeader, body, etc.).
 * 
 * Usage:
 * <pre>
 *   EDIUnmarshalResult&lt;PurchaseOrder&gt; result = EDIUnmarshaller.unmarshalResult(PurchaseOrder.class, reader);
 *   
 *   // Generate 997 from the parse result
 *   EDI997Generator.Result ack = EDI997Generator.generate(result);
 *   String ediOutput = ack.getEdiOutput();          // raw EDI 997 string
 *   String controlNumber = ack.getControlNumber();   // for tracking
 *   
 *   // Or generate from a successfully parsed object
 *   EDI997Generator.Result ack = EDI997Generator.generate(purchaseOrder);
 * </pre>
 * 
 * X12 997 structure produced:
 * <pre>
 *   ISA*...*~      (envelope header -- sender/receiver swapped)
 *   GS*FA*...*~    (group header -- functional ID = "FA")
 *   ST*997*...*~   (transaction set header)
 *   AK1*PO*...*~   (functional group response header)
 *   AK9*A*1*1*1*~  (functional group response trailer -- A=Accepted)
 *   SE*4*...*~     (transaction set trailer)
 *   GE*1*...*~     (group trailer)
 *   IEA*1*...*~    (envelope trailer)
 * </pre>
 */
public class EDI997Generator {

    private static final Logger LOG = LoggerFactory.getLogger(EDI997Generator.class);

    // AK9 Functional Group Acknowledge Codes
    public static final String ACCEPTED                  = "A";  // Accepted
    public static final String ACCEPTED_WITH_ERRORS      = "E";  // Accepted, But Errors Were Noted
    public static final String PARTIALLY_ACCEPTED        = "P";  // Partially Accepted
    public static final String REJECTED                  = "R";  // Rejected

    // AK5 Transaction Set Acknowledge Codes
    public static final String TS_ACCEPTED               = "A";  // Accepted
    public static final String TS_ACCEPTED_WITH_ERRORS   = "E";  // Accepted But Errors Were Noted
    public static final String TS_REJECTED               = "R";  // Rejected

    private EDI997Generator() {
        // seal
    }

    // ==================== Result wrapper ====================

    /**
     * Holds the generated 997 EDI output and metadata.
     */
    public static class Result {
        private String ediOutput;
        private String controlNumber;
        private String acknowledgeCode;
        private int segmentCount;
        private boolean success;
        private String error;

        public String getEdiOutput()        { return ediOutput; }
        public String getControlNumber()    { return controlNumber; }
        public String getAcknowledgeCode()  { return acknowledgeCode; }
        public int getSegmentCount()        { return segmentCount; }
        public boolean isSuccess()          { return success; }
        public String getError()            { return error; }

        void setEdiOutput(String v)         { this.ediOutput = v; }
        void setControlNumber(String v)     { this.controlNumber = v; }
        void setAcknowledgeCode(String v)   { this.acknowledgeCode = v; }
        void setSegmentCount(int v)         { this.segmentCount = v; }
        void setSuccess(boolean v)          { this.success = v; }
        void setError(String v)             { this.error = v; }

        @Override
        public String toString() {
            return "EDI997Result{success=" + success
                    + ", ack=" + acknowledgeCode
                    + ", controlNumber=" + controlNumber
                    + ", segments=" + segmentCount
                    + (error != null ? ", error=" + error : "")
                    + "}";
        }
    }

    // ==================== Envelope data holder ====================

    /**
     * Extracted envelope data from the source EDI message.
     * Used to build the 997 response with swapped sender/receiver.
     */
    static class EnvelopeData {
        // ISA fields
        String authQualifier;           // ISA01
        String authInfo;                // ISA02
        String secQualifier;            // ISA03
        String secInfo;                 // ISA04
        String senderIdQualifier;       // ISA05
        String senderId;                // ISA06
        String receiverIdQualifier;     // ISA07
        String receiverId;              // ISA08
        String interchangeControlId;    // ISA11
        String interchangeVersion;      // ISA12
        String interchangeControlNum;   // ISA13
        String ackRequested;            // ISA14
        String testIndicator;           // ISA15
        String subelementSep;           // ISA16

        // GS fields
        String functionalIdCode;        // GS01
        String appSenderCode;           // GS02
        String appReceiverCode;         // GS03
        String groupControlNumber;      // GS06
        String responsibleAgency;       // GS07
        String versionCode;             // GS08
    }

    // ==================== Public API ====================

    /**
     * Generates a 997 from an EDIUnmarshalResult.
     * Automatically determines the acknowledge code based on parse/validation status.
     * 
     * @param unmarshalResult the result from EDIUnmarshaller.unmarshalResult()
     * @return Result containing the 997 EDI string
     */
    public static <T> Result generate(EDIUnmarshalResult<T> unmarshalResult) {
        return generate(unmarshalResult, null);
    }

    /**
     * Generates a 997 from an EDIUnmarshalResult with a custom control number.
     * 
     * @param unmarshalResult the result from EDIUnmarshaller.unmarshalResult()
     * @param controlNumber   optional control number (auto-generated if null)
     * @return Result containing the 997 EDI string
     */
    public static <T> Result generate(EDIUnmarshalResult<T> unmarshalResult, String controlNumber) {
        Result result = new Result();

        if (unmarshalResult == null || unmarshalResult.getData() == null) {
            result.setError("Cannot generate 997: unmarshal result or data is null");
            return result;
        }

        // Determine acknowledge code from parse/validation status
        String ackCode;
        String tsAckCode;
        if (!unmarshalResult.isParsed()) {
            ackCode = REJECTED;
            tsAckCode = TS_REJECTED;
        } else if (unmarshalResult.hasErrors()) {
            ackCode = ACCEPTED_WITH_ERRORS;
            tsAckCode = TS_ACCEPTED_WITH_ERRORS;
        } else {
            ackCode = ACCEPTED;
            tsAckCode = TS_ACCEPTED;
        }

        return generateFrom(unmarshalResult.getData(), ackCode, tsAckCode, controlNumber);
    }

    /**
     * Generates a 997 from a successfully parsed EDI message object.
     * Assumes full acceptance (AK9=A, AK5=A).
     * 
     * @param ediMessage the parsed EDI message object (must have ISA/GS envelopes)
     * @return Result containing the 997 EDI string
     */
    public static Result generate(Object ediMessage) {
        return generateFrom(ediMessage, ACCEPTED, TS_ACCEPTED, null);
    }

    /**
     * Generates a 997 from a parsed EDI message object with explicit acknowledge codes.
     * 
     * @param ediMessage       the parsed EDI message object
     * @param groupAckCode     AK9 group acknowledge code (A/E/P/R)
     * @param txnSetAckCode    AK5 transaction set acknowledge code (A/E/R)
     * @param controlNumber    optional control number
     * @return Result containing the 997 EDI string
     */
    public static Result generate(Object ediMessage, String groupAckCode,
                                  String txnSetAckCode, String controlNumber) {
        return generateFrom(ediMessage, groupAckCode, txnSetAckCode, controlNumber);
    }

    // ==================== Core generation logic ====================

    private static Result generateFrom(Object ediMessage, String groupAckCode,
                                       String tsAckCode, String controlNumber) {
        Result result = new Result();

        try {
            // Extract envelope data via reflection
            EnvelopeData env = extractEnvelope(ediMessage);
            if (env == null) {
                result.setError("Cannot extract ISA/GS envelope from the source message. "
                        + "Ensure the class has envelopeHeader (ISA) and groupEnvelopeHeader (GS) fields.");
                return result;
            }

            // Generate control number if not provided
            if (controlNumber == null || controlNumber.trim().isEmpty()) {
                controlNumber = generateControlNumber();
            }
            result.setControlNumber(controlNumber);
            result.setAcknowledgeCode(groupAckCode);

            // Count transaction sets in source
            int txnSetCount = countTransactionSets(ediMessage);

            // Determine the transaction set ID code from the source ST segment
            String sourceTxnSetId = extractTransactionSetId(ediMessage);

            // Get delimiters
            char elemDelim = '*';
            char segDelim = '~';
            if (ediMessage.getClass().isAnnotationPresent(EDIMessage.class)) {
                EDIMessage ann = ediMessage.getClass().getAnnotation(EDIMessage.class);
                elemDelim = ann.elementDelimiter();
                segDelim = ann.segmentDelimiter();
            }
            // Also check interfaces for @EDIMessage
            for (Class<?> iface : ediMessage.getClass().getInterfaces()) {
                if (iface.isAnnotationPresent(EDIMessage.class)) {
                    EDIMessage ann = iface.getAnnotation(EDIMessage.class);
                    elemDelim = ann.elementDelimiter();
                    segDelim = ann.segmentDelimiter();
                    break;
                }
            }

            String e = String.valueOf(elemDelim);
            String s = String.valueOf(segDelim);

            // Format dates
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd");
            SimpleDateFormat timeFormat = new SimpleDateFormat("HHmm");
            Date now = new Date();
            String currentDate = dateFormat.format(now);
            String currentTime = timeFormat.format(now);
            String currentDateFull = new SimpleDateFormat("yyyyMMdd").format(now);

            // Pad control number to 9 chars
            String paddedControlNum = padLeft(controlNumber, 9, '0');

            StringBuilder sb = new StringBuilder();

            // === ISA (swap sender/receiver) ===
            sb.append("ISA").append(e);
            sb.append(env.authQualifier != null ? env.authQualifier : "00").append(e);
            sb.append(env.authInfo != null ? env.authInfo : padRight("", 10)).append(e);
            sb.append(env.secQualifier != null ? env.secQualifier : "00").append(e);
            sb.append(env.secInfo != null ? env.secInfo : padRight("", 10)).append(e);
            // SWAP: original receiver becomes sender
            sb.append(env.receiverIdQualifier != null ? env.receiverIdQualifier : "ZZ").append(e);
            sb.append(env.receiverId != null ? env.receiverId : padRight("", 15)).append(e);
            // SWAP: original sender becomes receiver
            sb.append(env.senderIdQualifier != null ? env.senderIdQualifier : "ZZ").append(e);
            sb.append(env.senderId != null ? env.senderId : padRight("", 15)).append(e);
            sb.append(currentDate).append(e);
            sb.append(currentTime).append(e);
            sb.append(env.interchangeControlId != null ? env.interchangeControlId : "U").append(e);
            sb.append(env.interchangeVersion != null ? env.interchangeVersion : "00401").append(e);
            sb.append(paddedControlNum).append(e);
            sb.append(env.ackRequested != null ? env.ackRequested : "0").append(e);
            sb.append(env.testIndicator != null ? env.testIndicator : "P").append(e);
            sb.append(env.subelementSep != null ? env.subelementSep : ">").append(s).append("\n");

            // === GS (swap sender/receiver, functional ID = FA) ===
            sb.append("GS").append(e);
            sb.append("FA").append(e);  // FA = Functional Acknowledgement
            // SWAP: original receiver becomes sender
            sb.append(env.appReceiverCode != null ? env.appReceiverCode : "").append(e);
            // SWAP: original sender becomes receiver
            sb.append(env.appSenderCode != null ? env.appSenderCode : "").append(e);
            sb.append(currentDateFull).append(e);
            sb.append(currentTime).append(e);
            sb.append(controlNumber).append(e);
            sb.append(env.responsibleAgency != null ? env.responsibleAgency : "X").append(e);
            sb.append(env.versionCode != null ? env.versionCode : "004010").append(s).append("\n");

            // === ST (Transaction Set Header) ===
            sb.append("ST").append(e);
            sb.append("997").append(e);
            sb.append(padLeft(controlNumber, 4, '0')).append(s).append("\n");

            int segCount = 2; // ST and SE are counted

            // === AK1 (Functional Group Response Header) ===
            sb.append("AK1").append(e);
            sb.append(env.functionalIdCode != null ? env.functionalIdCode : "PO").append(e);
            sb.append(env.groupControlNumber != null ? env.groupControlNumber : "1").append(s).append("\n");
            segCount++;

            // === AK2/AK5 pairs for each transaction set ===
            // For simplicity, we generate one AK2/AK5 pair per transaction set found
            int txnSetsAccepted = 0;
            for (int i = 0; i < Math.max(txnSetCount, 1); i++) {
                String tsControlNum = padLeft(String.valueOf(i + 1), 4, '0');

                // AK2 - Transaction Set Response Header
                sb.append("AK2").append(e);
                sb.append(sourceTxnSetId != null ? sourceTxnSetId : "850").append(e);
                sb.append(tsControlNum).append(s).append("\n");
                segCount++;

                // AK5 - Transaction Set Response Trailer
                sb.append("AK5").append(e);
                sb.append(tsAckCode).append(s).append("\n");
                segCount++;

                if (TS_ACCEPTED.equals(tsAckCode) || TS_ACCEPTED_WITH_ERRORS.equals(tsAckCode)) {
                    txnSetsAccepted++;
                }
            }

            // === AK9 (Functional Group Response Trailer) ===
            String totalSets = String.valueOf(Math.max(txnSetCount, 1));
            String receivedSets = totalSets;
            String acceptedSets = String.valueOf(txnSetsAccepted);

            sb.append("AK9").append(e);
            sb.append(groupAckCode).append(e);
            sb.append(totalSets).append(e);
            sb.append(receivedSets).append(e);
            sb.append(acceptedSets).append(s).append("\n");
            segCount++;

            // === SE (Transaction Set Trailer) ===
            sb.append("SE").append(e);
            sb.append(String.valueOf(segCount)).append(e);
            sb.append(padLeft(controlNumber, 4, '0')).append(s).append("\n");

            // === GE (Group Envelope Trailer) ===
            sb.append("GE").append(e);
            sb.append("1").append(e);  // 1 transaction set in this group
            sb.append(controlNumber).append(s).append("\n");

            // === IEA (Interchange Envelope Trailer) ===
            sb.append("IEA").append(e);
            sb.append("1").append(e);  // 1 group in this interchange
            sb.append(paddedControlNum).append(s).append("\n");

            result.setEdiOutput(sb.toString());
            result.setSegmentCount(segCount + 4); // ST..SE segments + ISA,GS,GE,IEA
            result.setSuccess(true);

            LOG.info("997 generated: ack={}, controlNumber={}, segments={}",
                    groupAckCode, controlNumber, result.getSegmentCount());

        } catch (Exception ex) {
            result.setError("997 generation failed: " + ex.getMessage());
            LOG.error("997 generation failed", ex);
        }

        return result;
    }

    // ==================== Reflection helpers ====================

    /**
     * Extracts ISA and GS envelope data from any EDI message object via reflection.
     * Looks for fields named envelopeHeader (ISA) and groupEnvelopeHeader (GS).
     */
    static EnvelopeData extractEnvelope(Object ediMessage) {
        if (ediMessage == null) return null;

        try {
            EnvelopeData env = new EnvelopeData();
            Class<?> clazz = ediMessage.getClass();

            // --- Extract ISA (InterchangeEnvelopeHeader) ---
            Object isa = getFieldValue(ediMessage, "envelopeHeader");
            if (isa != null) {
                env.authQualifier        = getStringField(isa, "authorizationInformationQualifier");
                env.authInfo             = getStringField(isa, "authorizationInformation");
                env.secQualifier         = getStringField(isa, "securityInformationQualifier");
                env.secInfo              = getStringField(isa, "securityInformation");
                env.senderIdQualifier    = getStringField(isa, "senderInterchangeIDQualifier");
                env.senderId             = getStringField(isa, "interchangeSenderID");
                env.receiverIdQualifier  = getStringField(isa, "receiverInterchangeIDQualifier");
                env.receiverId           = getStringField(isa, "interchangeReceiverID");
                env.interchangeControlId = getStringField(isa, "interchangeControlID");
                env.interchangeVersion   = getStringField(isa, "interchangeVersionNumber");
                env.interchangeControlNum = getStringField(isa, "interchangeControlNumber");
                env.ackRequested         = getStringField(isa, "iSAAcknowledgmentRequested");
                env.testIndicator        = getStringField(isa, "testIndicator");
                env.subelementSep        = getStringField(isa, "subelementSeparator");
            }

            // --- Extract GS (GroupEnvelopeHeader) ---
            Object gs = getFieldValue(ediMessage, "groupEnvelopeHeader");
            if (gs != null) {
                env.functionalIdCode  = getStringField(gs, "functionalIDCode");
                env.appSenderCode     = getStringField(gs, "applicationSendersCode");
                env.appReceiverCode   = getStringField(gs, "applicationReceiversCode");
                env.groupControlNumber = getStringField(gs, "groupControlNumber");
                env.responsibleAgency = getStringField(gs, "responsibleAgencyCode");
                env.versionCode       = getStringField(gs, "versionReleaseIndustryIdentifierCode");
            }

            // Validate we have at least some data
            if (isa == null && gs == null) {
                return null;
            }

            return env;

        } catch (Exception ex) {
            LOG.error("Failed to extract envelope data: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Counts the number of transaction sets (body elements) in the source message.
     */
    static int countTransactionSets(Object ediMessage) {
        try {
            Object body = getFieldValue(ediMessage, "body");
            if (body == null) {
                // Try common alternative field names
                body = getFieldValue(ediMessage, "purchaseOrderBody");
                if (body == null) {
                    body = getFieldValue(ediMessage, "invoiceBody");
                }
            }
            if (body instanceof Collection) {
                return ((Collection<?>) body).size();
            }
        } catch (Exception ex) {
            LOG.debug("Could not count transaction sets: {}", ex.getMessage());
        }
        return 1; // Default: assume 1 transaction set
    }

    /**
     * Extracts the transaction set identifier code (ST01) from the source message.
     * Walks into body -> header -> transactionSetHeader -> transactionSetIdentifierCode.
     */
    static String extractTransactionSetId(Object ediMessage) {
        try {
            // Try direct body collection
            Object body = getFieldValue(ediMessage, "body");
            if (body == null) {
                // Try common alternative field names
                for (String fieldName : new String[]{"purchaseOrderBody", "invoiceBody",
                        "acknowledgementBody", "shipmentBody"}) {
                    body = getFieldValue(ediMessage, fieldName);
                    if (body != null) break;
                }
            }

            // Get first element from collection
            Object firstBody = null;
            if (body instanceof Collection) {
                Collection<?> coll = (Collection<?>) body;
                if (!coll.isEmpty()) {
                    firstBody = coll.iterator().next();
                }
            } else if (body != null) {
                firstBody = body;
            }

            if (firstBody != null) {
                // Try header -> transactionSetHeader -> transactionSetIdentifierCode
                Object header = getFieldValue(firstBody, "header");
                if (header != null) {
                    Object stHeader = getFieldValue(header, "transactionSetHeader");
                    if (stHeader != null) {
                        String code = getStringField(stHeader, "transactionSetIdentifierCode");
                        if (code != null && !code.isEmpty()) {
                            return code;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LOG.debug("Could not extract transaction set ID: {}", ex.getMessage());
        }

        // Fallback: try to infer from GS01
        try {
            Object gs = getFieldValue(ediMessage, "groupEnvelopeHeader");
            if (gs != null) {
                String funcId = getStringField(gs, "functionalIDCode");
                if (funcId != null) {
                    return mapFunctionalIdToTxnSetId(funcId);
                }
            }
        } catch (Exception ex) {
            // ignore
        }

        return "850"; // Default fallback
    }

    /**
     * Maps GS01 Functional ID Code to the most common transaction set ID.
     */
    private static String mapFunctionalIdToTxnSetId(String funcId) {
        if (funcId == null) return "850";
        switch (funcId.toUpperCase()) {
            case "PO": return "850";
            case "IN": return "810";
            case "SH": return "856";
            case "PR": return "855";
            case "FA": return "997";
            case "SC": return "832";
            case "IB": return "846";
            case "AG": return "824";
            case "TX": return "864";
            case "OR": return "816";
            default:   return funcId;
        }
    }

    // ==================== Low-level reflection ====================

    private static Object getFieldValue(Object obj, String fieldName) {
        if (obj == null) return null;
        Class<?> clazz = obj.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(obj);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static String getStringField(Object obj, String fieldName) {
        Object val = getFieldValue(obj, fieldName);
        if (val == null) return null;
        return val.toString();
    }

    // ==================== String utilities ====================

    private static String generateControlNumber() {
        // Use timestamp-based control number (last 9 digits of epoch millis)
        long millis = System.currentTimeMillis();
        String num = String.valueOf(millis);
        if (num.length() > 9) {
            num = num.substring(num.length() - 9);
        }
        return num;
    }

    private static String padLeft(String str, int len, char padChar) {
        if (str == null) str = "";
        StringBuilder sb = new StringBuilder();
        for (int i = str.length(); i < len; i++) {
            sb.append(padChar);
        }
        sb.append(str);
        return sb.toString();
    }

    private static String padRight(String str, int len) {
        if (str == null) str = "";
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < len) {
            sb.append(' ');
        }
        return sb.toString();
    }
}

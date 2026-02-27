package javax.edi.bind.util;

import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lightweight EDI message metadata extractor.
 * Quickly extracts key header fields (ISA, GS, ST) from an EDI message
 * WITHOUT performing full unmarshalling.
 * 
 * Use cases:
 * - Route EDI messages based on sender/receiver/type before parsing
 * - Log and audit incoming messages
 * - Dashboard/monitoring: display message counts by type, sender, etc.
 * - Quick validation: check if message has required envelope segments
 * 
 * Usage:
 *   EDIMetadataExtractor.Metadata meta = EDIMetadataExtractor.extract(reader);
 *   String sender = meta.getSenderID();
 *   String poNumber = meta.get("BEG03");
 */
public class EDIMetadataExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(EDIMetadataExtractor.class);

    private EDIMetadataExtractor() {
        // seal
    }

    /**
     * Extracts metadata from an EDI message using default delimiters (* and ~).
     */
    public static Metadata extract(Reader reader) {
        return extract(reader, '*', '~');
    }

    /**
     * Extracts metadata from an EDI message with specified delimiters.
     */
    public static Metadata extract(Reader reader, char elementDelimiter, char segmentDelimiter) {
        Metadata metadata = new Metadata();

        try {
            SegmentIterator iterator = new SegmentIterator(reader, segmentDelimiter, true);
            int segmentCount = 0;

            while (iterator.hasNext()) {
                String segment = iterator.next();
                segmentCount++;

                if (StringUtils.isBlank(segment)) {
                    continue;
                }

                String[] elements = segment.split("\\" + String.valueOf(elementDelimiter), -1);
                if (elements.length == 0) {
                    continue;
                }

                String tag = elements[0].trim();

                switch (tag) {
                    case "ISA":
                        parseISA(elements, metadata);
                        break;
                    case "GS":
                        parseGS(elements, metadata);
                        break;
                    case "ST":
                        parseST(elements, metadata);
                        break;
                    case "BEG":
                        parseBEG(elements, metadata);
                        break;
                    case "BSN":
                        parseBSN(elements, metadata);
                        break;
                    case "BIG":
                        parseBIG(elements, metadata);
                        break;
                    case "GE":
                        // Group trailer — stop after header extraction
                        break;
                    case "IEA":
                        // Interchange trailer
                        break;
                    default:
                        break;
                }
            }

            metadata.setSegmentCount(segmentCount);

        } catch (Exception e) {
            LOG.error("Error extracting EDI metadata: " + e.getMessage(), e);
            metadata.setError(e.getMessage());
        }

        return metadata;
    }

    private static void parseISA(String[] elements, Metadata metadata) {
        if (elements.length > 5) {
            metadata.setSenderQualifier(elements[5].trim());
        }
        if (elements.length > 6) {
            metadata.setSenderID(elements[6].trim());
        }
        if (elements.length > 7) {
            metadata.setReceiverQualifier(elements[7].trim());
        }
        if (elements.length > 8) {
            metadata.setReceiverID(elements[8].trim());
        }
        if (elements.length > 9) {
            metadata.set("ISA09_Date", elements[9].trim());
        }
        if (elements.length > 10) {
            metadata.set("ISA10_Time", elements[10].trim());
        }
        if (elements.length > 12) {
            metadata.set("ISA12_VersionNumber", elements[12].trim());
        }
        if (elements.length > 13) {
            metadata.setInterchangeControlNumber(elements[13].trim());
        }
        if (elements.length > 15) {
            metadata.set("ISA15_UsageIndicator", elements[15].trim());
            String usage = elements[15].trim();
            metadata.setTestMode("T".equalsIgnoreCase(usage));
        }
    }

    private static void parseGS(String[] elements, Metadata metadata) {
        if (elements.length > 1) {
            metadata.setFunctionalGroupCode(elements[1].trim());
        }
        if (elements.length > 2) {
            metadata.set("GS02_ApplicationSender", elements[2].trim());
        }
        if (elements.length > 3) {
            metadata.set("GS03_ApplicationReceiver", elements[3].trim());
        }
        if (elements.length > 4) {
            metadata.set("GS04_Date", elements[4].trim());
        }
        if (elements.length > 6) {
            metadata.setGroupControlNumber(elements[6].trim());
        }
        if (elements.length > 8) {
            metadata.set("GS08_Version", elements[8].trim());
        }
    }

    private static void parseST(String[] elements, Metadata metadata) {
        if (elements.length > 1) {
            metadata.setTransactionSetCode(elements[1].trim());
            // Map code to friendly name
            String code = elements[1].trim();
            metadata.setTransactionSetName(getTransactionSetName(code));
        }
        if (elements.length > 2) {
            metadata.setTransactionSetControlNumber(elements[2].trim());
        }
    }

    private static void parseBEG(String[] elements, Metadata metadata) {
        // Purchase Order beginning segment
        if (elements.length > 1) {
            metadata.set("BEG01_PurposeCode", elements[1].trim());
        }
        if (elements.length > 2) {
            metadata.set("BEG02_OrderType", elements[2].trim());
        }
        if (elements.length > 3) {
            metadata.setPurchaseOrderNumber(elements[3].trim());
        }
        if (elements.length > 5) {
            metadata.set("BEG05_Date", elements[5].trim());
        }
    }

    private static void parseBSN(String[] elements, Metadata metadata) {
        // Ship Notice beginning segment
        if (elements.length > 2) {
            metadata.set("BSN02_ShipmentID", elements[2].trim());
        }
        if (elements.length > 3) {
            metadata.set("BSN03_Date", elements[3].trim());
        }
    }

    private static void parseBIG(String[] elements, Metadata metadata) {
        // Invoice beginning segment
        if (elements.length > 1) {
            metadata.set("BIG01_InvoiceDate", elements[1].trim());
        }
        if (elements.length > 2) {
            metadata.set("BIG02_InvoiceNumber", elements[2].trim());
        }
    }

    private static String getTransactionSetName(String code) {
        switch (code) {
            case "810": return "Invoice";
            case "820": return "Payment Order/Remittance Advice";
            case "824": return "Application Advice";
            case "830": return "Planning Schedule with Release Capability";
            case "832": return "Price/Sales Catalog";
            case "840": return "Request for Quotation";
            case "843": return "Response to Request for Quotation";
            case "846": return "Inventory Inquiry/Advice";
            case "849": return "Response to Product Transfer Account Adjustment";
            case "850": return "Purchase Order";
            case "855": return "Purchase Order Acknowledgment";
            case "856": return "Advance Ship Notice";
            case "860": return "Purchase Order Change Request";
            case "861": return "Receiving Advice/Acceptance Certificate";
            case "864": return "Text Message";
            case "870": return "Order Status Report";
            case "875": return "Grocery Products Purchase Order";
            case "880": return "Grocery Products Invoice";
            case "990": return "Response to a Load Tender";
            case "997": return "Functional Acknowledgment";
            case "999": return "Implementation Acknowledgment";
            default:    return "Unknown (" + code + ")";
        }
    }

    /**
     * Container for extracted EDI metadata.
     */
    public static class Metadata {
        private String senderQualifier;
        private String senderID;
        private String receiverQualifier;
        private String receiverID;
        private String interchangeControlNumber;
        private String functionalGroupCode;
        private String groupControlNumber;
        private String transactionSetCode;
        private String transactionSetName;
        private String transactionSetControlNumber;
        private String purchaseOrderNumber;
        private boolean testMode;
        private int segmentCount;
        private String error;
        private Map<String, String> additionalFields = new LinkedHashMap<>();

        // --- Core getters/setters ---

        public String getSenderQualifier() { return senderQualifier; }
        public void setSenderQualifier(String senderQualifier) { this.senderQualifier = senderQualifier; }

        public String getSenderID() { return senderID; }
        public void setSenderID(String senderID) { this.senderID = senderID; }

        public String getReceiverQualifier() { return receiverQualifier; }
        public void setReceiverQualifier(String receiverQualifier) { this.receiverQualifier = receiverQualifier; }

        public String getReceiverID() { return receiverID; }
        public void setReceiverID(String receiverID) { this.receiverID = receiverID; }

        public String getInterchangeControlNumber() { return interchangeControlNumber; }
        public void setInterchangeControlNumber(String num) { this.interchangeControlNumber = num; }

        public String getFunctionalGroupCode() { return functionalGroupCode; }
        public void setFunctionalGroupCode(String code) { this.functionalGroupCode = code; }

        public String getGroupControlNumber() { return groupControlNumber; }
        public void setGroupControlNumber(String num) { this.groupControlNumber = num; }

        public String getTransactionSetCode() { return transactionSetCode; }
        public void setTransactionSetCode(String code) { this.transactionSetCode = code; }

        public String getTransactionSetName() { return transactionSetName; }
        public void setTransactionSetName(String name) { this.transactionSetName = name; }

        public String getTransactionSetControlNumber() { return transactionSetControlNumber; }
        public void setTransactionSetControlNumber(String num) { this.transactionSetControlNumber = num; }

        public String getPurchaseOrderNumber() { return purchaseOrderNumber; }
        public void setPurchaseOrderNumber(String poNumber) { this.purchaseOrderNumber = poNumber; }

        public boolean isTestMode() { return testMode; }
        public void setTestMode(boolean testMode) { this.testMode = testMode; }

        public int getSegmentCount() { return segmentCount; }
        public void setSegmentCount(int segmentCount) { this.segmentCount = segmentCount; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        /**
         * Get any additional extracted field by key.
         */
        public String get(String key) {
            return additionalFields.get(key);
        }

        /**
         * Set an additional extracted field.
         */
        public void set(String key, String value) {
            additionalFields.put(key, value);
        }

        /**
         * Returns all extracted fields as a map.
         */
        public Map<String, String> getAllFields() {
            Map<String, String> all = new LinkedHashMap<>();
            if (senderID != null) all.put("SenderID", senderID);
            if (senderQualifier != null) all.put("SenderQualifier", senderQualifier);
            if (receiverID != null) all.put("ReceiverID", receiverID);
            if (receiverQualifier != null) all.put("ReceiverQualifier", receiverQualifier);
            if (interchangeControlNumber != null) all.put("InterchangeControlNumber", interchangeControlNumber);
            if (functionalGroupCode != null) all.put("FunctionalGroupCode", functionalGroupCode);
            if (groupControlNumber != null) all.put("GroupControlNumber", groupControlNumber);
            if (transactionSetCode != null) all.put("TransactionSetCode", transactionSetCode);
            if (transactionSetName != null) all.put("TransactionSetName", transactionSetName);
            if (transactionSetControlNumber != null) all.put("TransactionSetControlNumber", transactionSetControlNumber);
            if (purchaseOrderNumber != null) all.put("PurchaseOrderNumber", purchaseOrderNumber);
            all.put("TestMode", String.valueOf(testMode));
            all.put("SegmentCount", String.valueOf(segmentCount));
            all.putAll(additionalFields);
            return all;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("EDI Metadata {\n");
            for (Map.Entry<String, String> entry : getAllFields().entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            if (error != null) {
                sb.append("  ERROR: ").append(error).append("\n");
            }
            sb.append("}");
            return sb.toString();
        }
    }
}

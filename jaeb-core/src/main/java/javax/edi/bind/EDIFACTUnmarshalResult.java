package javax.edi.bind;

import java.util.ArrayList;
import java.util.List;

import javax.edi.bind.util.EDIValidationError;

/**
 * Result wrapper for EDIFACT unmarshalling.
 *
 * @param <T> the EDIFACT message type
 */
public class EDIFACTUnmarshalResult<T> {

    private T data;
    private int segmentCount;
    private boolean parsed;
    private String parseError;
    private long parseTimeMillis;
    private List<EDIValidationError> errors = new ArrayList<>();

    // EDIFACT envelope info
    private String senderIdentification;
    private String receiverIdentification;
    private String interchangeReference;
    private String messageReference;
    private String messageType;
    private String messageVersion;
    private String messageRelease;
    private String controllingAgency;

    private EDIFACTUnmarshaller.Delimiters delimiters;

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public int getSegmentCount() { return segmentCount; }
    public void setSegmentCount(int segmentCount) { this.segmentCount = segmentCount; }

    public boolean isParsed() { return parsed; }
    public void setParsed(boolean parsed) { this.parsed = parsed; }

    public boolean isValid() { return parsed && !hasErrors() && parseError == null; }

    public boolean hasErrors() { return !errors.isEmpty(); }

    public List<EDIValidationError> getErrors() { return errors; }

    public void addError(EDIValidationError error) { errors.add(error); }

    public String getParseError() { return parseError; }
    public void setParseError(String parseError) {
        this.parseError = parseError;
        this.parsed = false;
    }

    public long getParseTimeMillis() { return parseTimeMillis; }
    public void setParseTimeMillis(long parseTimeMillis) { this.parseTimeMillis = parseTimeMillis; }

    public String getSenderIdentification() { return senderIdentification; }
    public void setSenderIdentification(String v) { this.senderIdentification = v; }

    public String getReceiverIdentification() { return receiverIdentification; }
    public void setReceiverIdentification(String v) { this.receiverIdentification = v; }

    public String getInterchangeReference() { return interchangeReference; }
    public void setInterchangeReference(String v) { this.interchangeReference = v; }

    public String getMessageReference() { return messageReference; }
    public void setMessageReference(String v) { this.messageReference = v; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String v) { this.messageType = v; }

    public String getMessageVersion() { return messageVersion; }
    public void setMessageVersion(String v) { this.messageVersion = v; }

    public String getMessageRelease() { return messageRelease; }
    public void setMessageRelease(String v) { this.messageRelease = v; }

    public String getControllingAgency() { return controllingAgency; }
    public void setControllingAgency(String v) { this.controllingAgency = v; }

    public EDIFACTUnmarshaller.Delimiters getDelimiters() { return delimiters; }
    public void setDelimiters(EDIFACTUnmarshaller.Delimiters d) { this.delimiters = d; }

    public String getErrorSummary() {
        StringBuilder sb = new StringBuilder();
        if (parseError != null) sb.append("Parse error: ").append(parseError).append("\n");
        for (EDIValidationError e : errors) sb.append(e.toString()).append("\n");
        return sb.toString();
    }
}

package javax.edi.model.edifact;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;

import javax.edi.bind.EDIFACTMarshaller;
import javax.edi.bind.EDIFACTUnmarshalResult;
import javax.edi.bind.EDIFACTUnmarshaller;
import javax.edi.bind.VDAMarshaller;
import javax.edi.bind.VDAUnmarshalResult;
import javax.edi.bind.VDAUnmarshaller;
import javax.edi.model.edifact.delfor.d04a.DelforD04A;
import javax.edi.model.edifact.delfor.d96a.DelforD96A;
import javax.edi.model.edifact.delfor.d96a.segment.BeginningOfMessage;
import javax.edi.model.edifact.delfor.d96a.segment.DateTimePeriod;
import javax.edi.model.edifact.delfor.d96a.segment.InterchangeHeader;
import javax.edi.model.edifact.delfor.d96a.segment.LineItem;
import javax.edi.model.edifact.delfor.d96a.segment.LineItemGroup;
import javax.edi.model.edifact.delfor.d96a.segment.MessageHeader;
import javax.edi.model.edifact.delfor.d96a.segment.NameAndAddress;
import javax.edi.model.edifact.delfor.d96a.segment.Quantity;
import javax.edi.model.vda.vda4905.VDA4905Message;
import javax.edi.model.vda.vda4905.record.DeliveryCallRecord;
import javax.edi.model.vda.vda4905.record.HeaderRecord;
import javax.edi.model.vda.vda4905.record.MaterialRecord;
import javax.edi.model.vda.vda4905.record.TrailerRecord;
import javax.edi.model.vda.vda4984.VDA4984Message;
import javax.edi.model.vda.vda4984.record.DispatchHeader;
import javax.edi.model.vda.vda4984.record.DispatchItem;
import javax.edi.model.vda.vda4984.record.DispatchTrailer;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Test class for European automotive EDI/EDIFACT message formats.
 *
 * <p>Each segment now directly contains the individual composite sub-fields
 * (e.g. QTY has quantityQualifier, quantity, measureUnitQualifier)
 * using {@code @EDIComponent} — no wrapper classes, matching the X12 pattern.</p>
 */
public class EDIFactVDAMessageTest {

    private static final Logger LOG = LoggerFactory.getLogger(EDIFactVDAMessageTest.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    // ========================================================================
    // 1. VDA 4905 — Delivery Schedule
    // ========================================================================

    @Test
    public void testVDA4905_DeliverySchedule_Parse() {
        LOG.info("=== TEST: VDA 4905 — Delivery Schedule ===");
        StringBuilder sb = new StringBuilder();
        sb.append(padRecord("51101CUST00001SUPP00001260115000010001", 128)).append("\n");
        sb.append(padRecord("512BMW-7ER-BRAKE-PAD-001 SUPP-BP-4711            WRK001DL00100001260101000000150000", 128)).append("\n");
        sb.append(padRecord("5132601201000000001000F0000000001260118", 128)).append("\n");
        sb.append(padRecord("5132601271000000000500P0000000002260125", 128)).append("\n");
        sb.append(padRecord("5132602031000000000800P0000000003260201", 128)).append("\n");
        sb.append(padRecord("519CUST00001SUPP000012601150000004", 128)).append("\n");

        VDAUnmarshalResult<VDA4905Message> result = VDAUnmarshaller.unmarshal(
                VDA4905Message.class, new StringReader(sb.toString()));

        Assert.assertTrue("VDA 4905 should parse", result.isParsed());
        Assert.assertEquals(6, result.getRecordCount());
        Assert.assertEquals("511", result.getData().getHeader().getRecordType());
        Assert.assertEquals("CUST00001", result.getData().getHeader().getCustomerNumber());
        Assert.assertNotNull(result.getData().getMaterials());
        Assert.assertEquals(1, result.getData().getMaterials().size());
        Assert.assertTrue(result.getData().getDeliveryCalls().size() >= 2);
        Assert.assertEquals("519", result.getData().getTrailer().getRecordType());
        LOG.info("Record stats: {}", result.getRecordStats());
        LOG.info("VDA 4905 parse PASSED\n");
    }

    @Test
    public void testVDA4905_EmptyMessage() {
        LOG.info("=== TEST: VDA 4905 — Empty Message ===");
        VDAUnmarshalResult<VDA4905Message> result = VDAUnmarshaller.unmarshal(
                VDA4905Message.class, new StringReader(""));
        Assert.assertFalse(result.isParsed());
        LOG.info("Empty message error: {}", result.getParseError());
        LOG.info("VDA 4905 empty message test PASSED\n");
    }

    @Test
    public void testVDA4905_HeaderAndTrailerOnly() {
        LOG.info("=== TEST: VDA 4905 — Header + Trailer Only ===");
        StringBuilder sb = new StringBuilder();
        sb.append(padRecord("51101CUST00001SUPP00001260115000010001", 128)).append("\n");
        sb.append(padRecord("519CUST00001SUPP000012601150000002", 128)).append("\n");

        VDAUnmarshalResult<VDA4905Message> result = VDAUnmarshaller.unmarshal(
                VDA4905Message.class, new StringReader(sb.toString()));
        Assert.assertTrue(result.isParsed());
        Assert.assertNotNull(result.getData().getHeader());
        Assert.assertNotNull(result.getData().getTrailer());
        LOG.info("VDA 4905 header+trailer test PASSED\n");
    }

    // ========================================================================
    // 2. VDA 4984 — Dispatch Advice
    // ========================================================================

    @Test
    public void testVDA4984_DispatchAdvice_Parse() {
        LOG.info("=== TEST: VDA 4984 — Dispatch Advice ===");
        StringBuilder sb = new StringBuilder();
        sb.append(padRecord("72101CUST00002SUPP00002DN001234260115260116260117", 128)).append("\n");
        sb.append(padRecord(vdaField("722", 3) + vdaField("GEAR-SHAFT-ASSY-001", 22)
                + vdaField("GS-4711-REV-B", 22) + vdaField("000000005000", 12)
                + vdaField("PCS", 3) + vdaField("PLANT1", 6)
                + vdaField("CALL000001", 10) + vdaField("000000025000", 12), 128)).append("\n");
        sb.append(padRecord(vdaField("722", 3) + vdaField("BEARING-OUTER-002", 22)
                + vdaField("BRG-9988-REV-A", 22) + vdaField("000000010000", 12)
                + vdaField("PCS", 3) + vdaField("PLANT1", 6)
                + vdaField("CALL000002", 10) + vdaField("000000050000", 12), 128)).append("\n");
        sb.append(padRecord("723PLTCONT-EUR-000000001 00010000000025000000000020000", 128)).append("\n");
        sb.append(padRecord("723BOXCONT-KLT-000000002 00050000000005000000000004500", 128)).append("\n");
        sb.append(padRecord("729CUST00002SUPP00002DN0012340000005000060", 128)).append("\n");

        VDAUnmarshalResult<VDA4984Message> result = VDAUnmarshaller.unmarshal(
                VDA4984Message.class, new StringReader(sb.toString()));

        Assert.assertTrue(result.isParsed());
        Assert.assertEquals(6, result.getRecordCount());
        Assert.assertEquals("721", result.getData().getHeader().getRecordType());
        Assert.assertEquals("DN001234", result.getData().getHeader().getDeliveryNoteNumber());
        Assert.assertEquals(2, result.getData().getItems().size());
        Assert.assertEquals("PCS", result.getData().getItems().get(0).getUnitOfMeasure().trim());
        Assert.assertTrue(result.getData().getPackaging().size() >= 1);
        Assert.assertEquals("729", result.getData().getTrailer().getRecordType());
        LOG.info("Record stats: {}", result.getRecordStats());
        LOG.info("VDA 4984 parse PASSED\n");
    }

    @Test
    public void testVDA4984_SingleItemNoPackaging() {
        LOG.info("=== TEST: VDA 4984 — Single Item, No Packaging ===");
        StringBuilder sb = new StringBuilder();
        sb.append(padRecord("72101CUST00003SUPP00003DN009999260201260202260203", 128)).append("\n");
        sb.append(padRecord(vdaField("722", 3) + vdaField("TURBO-HOSE-ASSY-003", 22)
                + vdaField("TH-3333-REV-C", 22) + vdaField("000000002500", 12)
                + vdaField("PCS", 3) + vdaField("PLANT2", 6)
                + vdaField("CALL000010", 10) + vdaField("000000075000", 12), 128)).append("\n");
        sb.append(padRecord("729CUST00003SUPP00003DN0099990000002000020", 128)).append("\n");

        VDAUnmarshalResult<VDA4984Message> result = VDAUnmarshaller.unmarshal(
                VDA4984Message.class, new StringReader(sb.toString()));
        Assert.assertTrue(result.isParsed());
        Assert.assertEquals(1, result.getData().getItems().size());
        LOG.info("VDA 4984 single item test PASSED\n");
    }

    // ========================================================================
    // 3. EDIFACT DELFOR D96A
    // ========================================================================

    @Test
    public void testDelforD96A_Parse() {
        LOG.info("=== TEST: EDIFACT DELFOR D96A ===");
        String msg =
                "UNA:+.? '" +
                "UNB+UNOC:3+BUYER001:14+SUPPLIER001:14+260115:1430+00000000000001'" +
                "UNH+1+DELFOR:D:96A:UN'" +
                "BGM+241+DELFOR-20260115-001+9'" +
                "DTM+137:20260115:102'" +
                "DTM+2:20260115:102'" +
                "DTM+36:20260315:102'" +
                "NAD+BY+BUYER001::92'" +
                "NAD+SU+SUPPLIER001::92'" +
                "NAD+ST+PLANT-WRK01::92'" +
                "LIN+1++BMW-7ER-BRAKE-PAD:IN'" +
                "QTY+1:1000:PCE'" +
                "LIN+2++GEAR-SHAFT-ASSY-001:IN'" +
                "QTY+1:500:PCE'" +
                "UNT+13+1'" +
                "UNZ+1+00000000000001'";

        EDIFACTUnmarshalResult<DelforD96A> result = EDIFACTUnmarshaller.unmarshal(
                DelforD96A.class, new StringReader(msg));

        Assert.assertTrue(result.isParsed());
        Assert.assertEquals("DELFOR", result.getMessageType());
        Assert.assertEquals("96A", result.getMessageRelease());
        DelforD96A d = result.getData();

        // BGM — flattened C002
        Assert.assertEquals("241", d.getBeginningOfMessage().getDocumentNameCode());
        Assert.assertEquals("DELFOR-20260115-001", d.getBeginningOfMessage().getDocumentNumber());
        Assert.assertEquals("9", d.getBeginningOfMessage().getMessageFunctionCode());

        // DTM — flattened C507
        Assert.assertEquals(3, d.getDateTimePeriods().size());
        Assert.assertEquals("137", d.getDateTimePeriods().get(0).getDateTimePeriodQualifier());
        Assert.assertEquals("20260115", d.getDateTimePeriods().get(0).getDateTimePeriodValue());
        Assert.assertEquals("102", d.getDateTimePeriods().get(0).getDateTimePeriodFormatQualifier());
        LOG.info("DTM[0]: qualifier={}, value={}, format={}",
                d.getDateTimePeriods().get(0).getDateTimePeriodQualifier(),
                d.getDateTimePeriods().get(0).getDateTimePeriodValue(),
                d.getDateTimePeriods().get(0).getDateTimePeriodFormatQualifier());

        // NAD — flattened C082
        Assert.assertEquals(3, d.getNameAndAddresses().size());
        Assert.assertEquals("BY", d.getNameAndAddresses().get(0).getPartyQualifier());
        Assert.assertEquals("BUYER001", d.getNameAndAddresses().get(0).getPartyIdIdentification());
        Assert.assertEquals("92", d.getNameAndAddresses().get(0).getCodeListResponsibleAgency());
        LOG.info("NAD[0]: qualifier={}, partyId={}, agency={}",
                d.getNameAndAddresses().get(0).getPartyQualifier(),
                d.getNameAndAddresses().get(0).getPartyIdIdentification(),
                d.getNameAndAddresses().get(0).getCodeListResponsibleAgency());

        // LIN — flattened C212
        Assert.assertTrue(d.getLineItems().size() >= 1);
        Assert.assertEquals("1", d.getLineItems().get(0).getLineItemNumber());
        Assert.assertEquals("BMW-7ER-BRAKE-PAD", d.getLineItems().get(0).getItemNumber());
        Assert.assertEquals("IN", d.getLineItems().get(0).getItemNumberType());
        LOG.info("LIN[0]: itemNo={}, type={}", d.getLineItems().get(0).getItemNumber(),
                d.getLineItems().get(0).getItemNumberType());

        // QTY — flattened C186
        Assert.assertEquals("1", d.getQuantities().get(0).getQuantityQualifier());
        Assert.assertEquals("1000", d.getQuantities().get(0).getQuantity());
        Assert.assertEquals("PCE", d.getQuantities().get(0).getMeasureUnitQualifier());
        LOG.info("QTY[0]: qualifier={}, qty={}, uom={}",
                d.getQuantities().get(0).getQuantityQualifier(),
                d.getQuantities().get(0).getQuantity(),
                d.getQuantities().get(0).getMeasureUnitQualifier());

        LOG.info("DELFOR D96A parse PASSED\n");
    }

    @Test
    public void testDelforD96A_WithoutUNA() {
        LOG.info("=== TEST: DELFOR D96A — Without UNA ===");
        String msg = "UNB+UNOC:3+SENDER:ZZ+RECEIVER:ZZ+260115:1200+REF001'" +
                "UNH+1+DELFOR:D:96A:UN'BGM+241+DOC001+9'UNT+3+1'UNZ+1+REF001'";
        EDIFACTUnmarshalResult<DelforD96A> result = EDIFACTUnmarshaller.unmarshal(
                DelforD96A.class, new StringReader(msg));
        Assert.assertTrue(result.isParsed());
        LOG.info("DELFOR D96A without UNA test PASSED\n");
    }

    @Test
    public void testDelforD96A_MinimalMessage() {
        LOG.info("=== TEST: DELFOR D96A — Minimal ===");
        String msg = "UNA:+.? 'UNB+UNOC:3+S:ZZ+R:ZZ+260115:0800+MINREF'" +
                "UNH+1+DELFOR:D:96A:UN'BGM+241+MIN-DOC+9'UNT+3+1'UNZ+1+MINREF'";
        EDIFACTUnmarshalResult<DelforD96A> result = EDIFACTUnmarshaller.unmarshal(
                DelforD96A.class, new StringReader(msg));
        Assert.assertTrue(result.isParsed());
        Assert.assertEquals("241", result.getData().getBeginningOfMessage().getDocumentNameCode());
        LOG.info("DELFOR D96A minimal test PASSED\n");
    }

    // ========================================================================
    // 4. EDIFACT DELFOR D04A
    // ========================================================================

    @Test
    public void testDelforD04A_Parse() throws IOException {
        LOG.info("=== TEST: EDIFACT DELFOR D04A ===");
        String msg1 =
                "UNA:+.? '" +
                "UNB+UNOC:3+VWAG:ZZZ+BOSCH-SUPP:ZZZ+260115:1530+CTRL-D04A-001'" +
                "UNH+1+DELFOR:D:04A:UN'" +
                "BGM+241+DELFOR-D04A-20260115+9'" +
                "DTM+137:20260115:102'" +
                "DTM+2:20260115:102'" +
                "DTM+36:20260615:102'" +
                "NAD+BY+VWAG::92++++Volkswagen AG'" +
                "NAD+SU+BOSCH-SUPP::92++++Robert Bosch GmbH'" +
                "NAD+ST+VW-WOLFSBURG::92++++Werk Wolfsburg'" +
                "NAD+DP+DOCK-A7::92++++Dock A7 Halle 3'" +
                "LIN+1++ABS-SENSOR-FRONT-L:IN'" +
                "QTY+1:2000:PCE'" +
                "QTY+3:15000:PCE'" +
                "LIN+2++ABS-SENSOR-FRONT-R:IN'" +
                "QTY+1:2000:PCE'" +
                "QTY+3:15000:PCE'" +
                "LIN+3++ESP-CONTROL-UNIT:IN'" +
                "QTY+1:800:PCE'" +
                "UNT+19+1'" +
                "UNZ+1+CTRL-D04A-001'";
        
        String msg = FileUtils.readFileToString(new File("D:\\Projects\\InApex\\DELFOR_D04a_5500001034_test.txt"));

        EDIFACTUnmarshalResult<DelforD04A> result = EDIFACTUnmarshaller.unmarshal(
                DelforD04A.class, new StringReader(msg));

        Assert.assertTrue(result.isParsed());
        Assert.assertEquals("04A", result.getMessageRelease());
        DelforD04A d = result.getData();

        Assert.assertEquals("241", d.getBeginningOfMessage().getDocumentNameCode());
        Assert.assertEquals(3, d.getDateTimePeriods().size());
        Assert.assertEquals("137", d.getDateTimePeriods().get(0).getDateTimePeriodQualifier());
        Assert.assertEquals(4, d.getNameAndAddresses().size());
        Assert.assertEquals("VWAG", d.getNameAndAddresses().get(0).getPartyIdIdentification());
        Assert.assertEquals("1", d.getQuantities().get(0).getQuantityQualifier());
        Assert.assertEquals("2000", d.getQuantities().get(0).getQuantity());
        Assert.assertEquals("PCE", d.getQuantities().get(0).getMeasureUnitQualifier());

        LOG.info("DELFOR D04A parse PASSED\n");
    }

    @Test
    public void testDelforD04A_WithoutUNA() {
        LOG.info("=== TEST: DELFOR D04A — Without UNA ===");
        String msg = "UNB+UNOC:3+BMW:ZZZ+CONTI:ZZZ+260115:0900+D04A-NO-UNA'" +
                "UNH+1+DELFOR:D:04A:UN'BGM+241+DOC-D04A-002+9'UNT+3+1'UNZ+1+D04A-NO-UNA'";
        EDIFACTUnmarshalResult<DelforD04A> result = EDIFACTUnmarshaller.unmarshal(
                DelforD04A.class, new StringReader(msg));
        Assert.assertTrue(result.isParsed());
        LOG.info("DELFOR D04A without UNA test PASSED\n");
    }

    @Test
    public void testDelforD04A_MinimalMessage() {
        LOG.info("=== TEST: DELFOR D04A — Minimal ===");
        String msg = "UNA:+.? 'UNB+UNOC:3+S:ZZ+R:ZZ+260115:0800+MINREF04'" +
                "UNH+1+DELFOR:D:04A:UN'BGM+241+MIN-D04A+9'UNT+3+1'UNZ+1+MINREF04'";
        EDIFACTUnmarshalResult<DelforD04A> result = EDIFACTUnmarshaller.unmarshal(
                DelforD04A.class, new StringReader(msg));
        Assert.assertTrue(result.isParsed());
        Assert.assertEquals("241", result.getData().getBeginningOfMessage().getDocumentNameCode());
        LOG.info("DELFOR D04A minimal test PASSED\n");
    }

    // ========================================================================
    // 5. Cross-format & Annotation Tests
    // ========================================================================

    @Test
    public void testAnnotationsPresent() {
        LOG.info("=== TEST: Annotation Verification ===");
        Assert.assertEquals("4905",
                VDA4905Message.class.getAnnotation(javax.edi.bind.annotations.VDAMessage.class).standard());
        Assert.assertEquals("4984",
                VDA4984Message.class.getAnnotation(javax.edi.bind.annotations.VDAMessage.class).standard());
        Assert.assertEquals("DELFOR",
                DelforD96A.class.getAnnotation(javax.edi.bind.annotations.EDIFACTMessage.class).type());
        Assert.assertEquals("D96A",
                DelforD96A.class.getAnnotation(javax.edi.bind.annotations.EDIFACTMessage.class).version());
        Assert.assertEquals("D04A",
                DelforD04A.class.getAnnotation(javax.edi.bind.annotations.EDIFACTMessage.class).version());
        LOG.info("Annotation verification PASSED\n");
    }

    @Test
    public void testDelfor_D96A_vs_D04A_EnvelopeConsistency() {
        LOG.info("=== TEST: D96A vs D04A — Envelope Consistency ===");
        String tmpl = "UNA:+.? 'UNB+UNOC:3+SENDER:14+RECEIVER:14+260115:1000+XREF001'" +
                "UNH+1+DELFOR:D:%s:UN'BGM+241+DOC-CROSS+9'UNT+3+1'UNZ+1+XREF001'";
        EDIFACTUnmarshalResult<DelforD96A> r96 = EDIFACTUnmarshaller.unmarshal(
                DelforD96A.class, new StringReader(String.format(tmpl, "96A")));
        EDIFACTUnmarshalResult<DelforD04A> r04 = EDIFACTUnmarshaller.unmarshal(
                DelforD04A.class, new StringReader(String.format(tmpl, "04A")));
        Assert.assertTrue(r96.isParsed());
        Assert.assertTrue(r04.isParsed());
        Assert.assertEquals(r96.getSenderIdentification(), r04.getSenderIdentification());
        Assert.assertEquals(r96.getReceiverIdentification(), r04.getReceiverIdentification());
        LOG.info("Envelope consistency PASSED\n");
    }

    // ========================================================================
    // 6. Real-world file parsing
    // ========================================================================

    @Test
    public void testDelforD96A_RealWorldFile_Parse() {
        LOG.info("=== TEST: DELFOR D96A — Real-world file ===");
        InputStreamReader reader = new InputStreamReader(
                this.getClass().getClassLoader().getResourceAsStream("DELFORD96A.edi"));
        Assert.assertNotNull(reader);

        EDIFACTUnmarshalResult<DelforD96A> result = EDIFACTUnmarshaller.unmarshal(
                DelforD96A.class, reader);

        Assert.assertTrue(result.isParsed());
        Assert.assertEquals("O093100000875450157AUGES", result.getSenderIdentification());
        Assert.assertEquals("PARTNERADDRESS", result.getReceiverIdentification());
        DelforD96A d = result.getData();

        // BGM — real-world has BGM+241:::DELIVERY FORECAST+7+5'
        Assert.assertEquals("241", d.getBeginningOfMessage().getDocumentNameCode());
        Assert.assertEquals("DELIVERY FORECAST", d.getBeginningOfMessage().getDocumentName());
        Assert.assertEquals("7", d.getBeginningOfMessage().getDocumentNumber());
        Assert.assertEquals("5", d.getBeginningOfMessage().getMessageFunctionCode());
        LOG.info("BGM: code={}, name={}, docNo={}, func={}",
                d.getBeginningOfMessage().getDocumentNameCode(),
                d.getBeginningOfMessage().getDocumentName(),
                d.getBeginningOfMessage().getDocumentNumber(),
                d.getBeginningOfMessage().getMessageFunctionCode());

        // DTM
        Assert.assertEquals("137", d.getDateTimePeriods().get(0).getDateTimePeriodQualifier());
        Assert.assertEquals("201309160000", d.getDateTimePeriods().get(0).getDateTimePeriodValue());
        Assert.assertEquals("203", d.getDateTimePeriods().get(0).getDateTimePeriodFormatQualifier());

        // RFF — flattened C506
        Assert.assertNotNull(d.getReferences());
        Assert.assertEquals("ADE", d.getReferences().get(0).getReferenceQualifier());
        Assert.assertEquals("0000123789", d.getReferences().get(0).getReferenceNumber());
        LOG.info("RFF[0]: qualifier={}, number={}",
                d.getReferences().get(0).getReferenceQualifier(),
                d.getReferences().get(0).getReferenceNumber());

        // NAD
        Assert.assertEquals("SE", d.getNameAndAddresses().get(0).getPartyQualifier());
        Assert.assertEquals("629914", d.getNameAndAddresses().get(0).getPartyIdIdentification());
        Assert.assertEquals("92", d.getNameAndAddresses().get(0).getCodeListResponsibleAgency());

        // LIN
        if (d.getLineItems() != null && !d.getLineItems().isEmpty()) {
            Assert.assertEquals("2437783012", d.getLineItems().get(0).getItemNumber());
            Assert.assertEquals("IN", d.getLineItems().get(0).getItemNumberType());
        }

        // QTY
        if (d.getQuantities() != null && !d.getQuantities().isEmpty()) {
            Assert.assertEquals("70", d.getQuantities().get(0).getQuantityQualifier());
            Assert.assertEquals("375", d.getQuantities().get(0).getQuantity());
            Assert.assertEquals("PCE", d.getQuantities().get(0).getMeasureUnitQualifier());
            LOG.info("QTY[0]: qualifier={}, qty={}, uom={}",
                    d.getQuantities().get(0).getQuantityQualifier(),
                    d.getQuantities().get(0).getQuantity(),
                    d.getQuantities().get(0).getMeasureUnitQualifier());
        }

        LOG.info("Total segments: {}", result.getSegmentCount());
        LOG.info("DELFOR D96A real-world parse PASSED\n");
    }

    // ========================================================================
    // 7. EDI -> JSON Conversion Tests
    // ========================================================================

    @Test
    public void testVDA4905_ToJSON() {
        LOG.info("=== TEST: VDA 4905 -> JSON ===");
        StringBuilder sb = new StringBuilder();
        sb.append(padRecord("51101CUST00001SUPP00001260115000010001", 128)).append("\n");
        sb.append(padRecord("512BMW-7ER-BRAKE-PAD-001 SUPP-BP-4711            WRK001DL00100001260101000000150000", 128)).append("\n");
        sb.append(padRecord("5132601201000000001000F0000000001260118", 128)).append("\n");
        sb.append(padRecord("5132601271000000000500P0000000002260125", 128)).append("\n");
        sb.append(padRecord("519CUST00001SUPP000012601150000004", 128)).append("\n");

        VDAUnmarshalResult<VDA4905Message> result = VDAUnmarshaller.unmarshal(
                VDA4905Message.class, new StringReader(sb.toString()));
        Assert.assertTrue(result.isParsed());

        String json = GSON.toJson(result.getData());
        JsonObject j = JsonParser.parseString(json).getAsJsonObject();
        Assert.assertTrue(j.has("header"));
        Assert.assertTrue(j.has("materials"));
        Assert.assertTrue(j.has("deliveryCalls"));
        Assert.assertEquals("511", j.getAsJsonObject("header").get("recordType").getAsString());
        LOG.info("VDA 4905 JSON:\n{}", json);
        LOG.info("VDA 4905 -> JSON PASSED\n");
    }

    @Test
    public void testVDA4984_ToJSON() {
        LOG.info("=== TEST: VDA 4984 -> JSON ===");
        StringBuilder sb = new StringBuilder();
        sb.append(padRecord("72101CUST00002SUPP00002DN001234260115260116260117", 128)).append("\n");
        sb.append(padRecord(vdaField("722", 3) + vdaField("GEAR-SHAFT-ASSY-001", 22)
                + vdaField("GS-4711-REV-B", 22) + vdaField("000000005000", 12)
                + vdaField("PCS", 3) + vdaField("PLANT1", 6)
                + vdaField("CALL000001", 10) + vdaField("000000025000", 12), 128)).append("\n");
        sb.append(padRecord("723PLTCONT-EUR-000000001 00010000000025000000000020000", 128)).append("\n");
        sb.append(padRecord("729CUST00002SUPP00002DN0012340000005000060", 128)).append("\n");

        VDAUnmarshalResult<VDA4984Message> result = VDAUnmarshaller.unmarshal(
                VDA4984Message.class, new StringReader(sb.toString()));
        Assert.assertTrue(result.isParsed());

        String json = GSON.toJson(result.getData());
        JsonObject j = JsonParser.parseString(json).getAsJsonObject();
        Assert.assertEquals("PCS", j.getAsJsonArray("items").get(0)
                .getAsJsonObject().get("unitOfMeasure").getAsString());
        LOG.info("VDA 4984 JSON:\n{}", json);
        LOG.info("VDA 4984 -> JSON PASSED\n");
    }

    @Test
    public void testDelforD96A_ToJSON() {
        LOG.info("=== TEST: DELFOR D96A -> JSON ===");
        String msg =
                "UNA:+.? '" +
                "UNB+UNOC:3+BUYER001:14+SUPPLIER001:14+260115:1430+00000000000001'" +
                "UNH+1+DELFOR:D:96A:UN'" +
                "BGM+241+DELFOR-20260115-001+9'" +
                "DTM+137:20260115:102'" +
                "DTM+2:20260115:102'" +
                "NAD+BY+BUYER001::92'" +
                "NAD+SU+SUPPLIER001::92'" +
                "LIN+1++BMW-7ER-BRAKE-PAD:IN'" +
                "QTY+1:1000:PCE'" +
                "UNT+10+1'" +
                "UNZ+1+00000000000001'";

        EDIFACTUnmarshalResult<DelforD96A> result = EDIFACTUnmarshaller.unmarshal(
                DelforD96A.class, new StringReader(msg));
        Assert.assertTrue(result.isParsed());

        String json = GSON.toJson(result.getData());
        JsonObject j = JsonParser.parseString(json).getAsJsonObject();

        // QTY inside lineItemGroups[0].quantities[0]
        JsonObject lig = j.getAsJsonArray("lineItemGroups").get(0).getAsJsonObject();
        JsonObject qty = lig.getAsJsonArray("quantities").get(0).getAsJsonObject();
        Assert.assertEquals("1", qty.get("quantityQualifier").getAsString());
        Assert.assertEquals("1000", qty.get("quantity").getAsString());
        Assert.assertEquals("PCE", qty.get("measureUnitQualifier").getAsString());

        // DTM flat in JSON
        JsonObject dtm = j.getAsJsonArray("dateTimePeriods").get(0).getAsJsonObject();
        Assert.assertEquals("137", dtm.get("dateTimePeriodQualifier").getAsString());
        Assert.assertEquals("20260115", dtm.get("dateTimePeriodValue").getAsString());
        Assert.assertEquals("102", dtm.get("dateTimePeriodFormatQualifier").getAsString());

        // BGM flat in JSON
        JsonObject bgm = j.getAsJsonObject("beginningOfMessage");
        Assert.assertEquals("241", bgm.get("documentNameCode").getAsString());

        // NAD flat in JSON
        JsonObject nad0 = j.getAsJsonArray("nameAndAddresses").get(0).getAsJsonObject();
        Assert.assertEquals("BY", nad0.get("partyQualifier").getAsString());
        Assert.assertEquals("BUYER001", nad0.get("partyIdIdentification").getAsString());

        // LIN inside lineItemGroups[0].lineItem
        JsonObject lin0 = lig.getAsJsonObject("lineItem");
        Assert.assertEquals("BMW-7ER-BRAKE-PAD", lin0.get("itemNumber").getAsString());
        Assert.assertEquals("IN", lin0.get("itemNumberType").getAsString());

        LOG.info("DELFOR D96A JSON:\n{}", json);
        LOG.info("DELFOR D96A -> JSON PASSED\n");
    }

    @Test
    public void testDelforD04A_ToJSON() {
        LOG.info("=== TEST: DELFOR D04A -> JSON ===");
        String msg =
                "UNA:+.? '" +
                "UNB+UNOC:3+VWAG:ZZZ+BOSCH-SUPP:ZZZ+260115:1530+CTRL-D04A-001'" +
                "UNH+1+DELFOR:D:04A:UN'" +
                "BGM+241+DELFOR-D04A-20260115+9'" +
                "DTM+137:20260115:102'" +
                "NAD+BY+VWAG::92++++Volkswagen AG'" +
                "NAD+SU+BOSCH-SUPP::92++++Robert Bosch GmbH'" +
                "LIN+1++ABS-SENSOR-FRONT-L:IN'" +
                "QTY+1:2000:PCE'" +
                "UNT+9+1'" +
                "UNZ+1+CTRL-D04A-001'";

        EDIFACTUnmarshalResult<DelforD04A> result = EDIFACTUnmarshaller.unmarshal(
                DelforD04A.class, new StringReader(msg));
        Assert.assertTrue(result.isParsed());

        String json = GSON.toJson(result.getData());
        JsonObject j = JsonParser.parseString(json).getAsJsonObject();
        JsonObject qty = j.getAsJsonArray("quantities").get(0).getAsJsonObject();
        Assert.assertEquals("1", qty.get("quantityQualifier").getAsString());
        Assert.assertEquals("2000", qty.get("quantity").getAsString());
        Assert.assertEquals("PCE", qty.get("measureUnitQualifier").getAsString());

        LOG.info("DELFOR D04A JSON:\n{}", json);
        LOG.info("DELFOR D04A -> JSON PASSED\n");
    }

    @Test
    public void testDelforD96A_RealWorldFile_ToJSON() {
        LOG.info("=== TEST: DELFOR D96A Real-world -> JSON ===");
        InputStreamReader reader = new InputStreamReader(
                this.getClass().getClassLoader().getResourceAsStream("DELFORD96A.edi"));
        Assert.assertNotNull(reader);

        EDIFACTUnmarshalResult<DelforD96A> result = EDIFACTUnmarshaller.unmarshal(
                DelforD96A.class, reader);
        Assert.assertTrue(result.isParsed());

        String json = GSON.toJson(result.getData());
        JsonObject j = JsonParser.parseString(json).getAsJsonObject();

        // BGM flat
        JsonObject bgm = j.getAsJsonObject("beginningOfMessage");
        Assert.assertEquals("241", bgm.get("documentNameCode").getAsString());
        Assert.assertEquals("DELIVERY FORECAST", bgm.get("documentName").getAsString());

        // QTY inside lineItemGroups[0].quantities[0]
        JsonObject lig0 = j.getAsJsonArray("lineItemGroups").get(0).getAsJsonObject();
        JsonObject qty0 = lig0.getAsJsonArray("quantities").get(0).getAsJsonObject();
        Assert.assertEquals("70", qty0.get("quantityQualifier").getAsString());
        Assert.assertEquals("375", qty0.get("quantity").getAsString());
        Assert.assertEquals("PCE", qty0.get("measureUnitQualifier").getAsString());

        // RFF at header level
        JsonObject rff0 = j.getAsJsonArray("references").get(0).getAsJsonObject();
        Assert.assertEquals("ADE", rff0.get("referenceQualifier").getAsString());
        Assert.assertEquals("0000123789", rff0.get("referenceNumber").getAsString());

        // Full envelope JSON
        JsonObject full = new JsonObject();
        full.addProperty("messageFormat", "EDIFACT");
        full.addProperty("messageType", "DELFOR");
        full.addProperty("messageVersion", "D96A");
        full.addProperty("sender", result.getSenderIdentification());
        full.addProperty("receiver", result.getReceiverIdentification());
        full.addProperty("interchangeReference", result.getInterchangeReference());
        full.addProperty("segmentCount", result.getSegmentCount());
        full.addProperty("parseTimeMs", result.getParseTimeMillis());
        full.add("message", j);

        LOG.info("Full JSON:\n{}", GSON.toJson(full));
        LOG.info("Real-world DELFOR D96A -> JSON PASSED\n");
    }

    // ========================================================================
    // 8. Marshal Tests — Generate EDI from Java Objects
    // ========================================================================

    @Test
    public void testVDA4905_Marshal() {
        LOG.info("=== TEST: VDA 4905 — Marshal ===");

        // Build message programmatically
        VDA4905Message msg = new VDA4905Message();

        HeaderRecord header = new HeaderRecord();
        header.setRecordType("511");
        header.setVersion("01");
        header.setCustomerNumber("CUST00001");
        header.setSupplierNumber("SUPP00001");
        header.setTransferDate("260115");
        header.setTransferNumber("00001");
        header.setScheduleNumber("0001");
        msg.setHeader(header);

        MaterialRecord mat = new MaterialRecord();
        mat.setRecordType("512");
        mat.setBuyerPartNumber("BRAKE-PAD-001");
        mat.setSupplierPartNumber("BP-4711");
        msg.setMaterials(Arrays.asList(mat));

        DeliveryCallRecord call = new DeliveryCallRecord();
        call.setRecordType("513");
        call.setDeliveryDate("260120");
        call.setDeliveryTimeCode("1");
        call.setDeliveryQuantity("000000001000");
        call.setCallOffType("F");
        call.setDeliveryInstruction("0000000001");
        call.setShippingDate("260118");
        msg.setDeliveryCalls(Arrays.asList(call));

        TrailerRecord trailer = new TrailerRecord();
        trailer.setRecordType("519");
        trailer.setCustomerNumber("CUST00001");
        trailer.setSupplierNumber("SUPP00001");
        trailer.setTransferDate("260115");
        trailer.setDataRecordCount("0000004");
        msg.setTrailer(trailer);

        // Marshal — standard fixed-width (no separators, per VDA spec)
        String edi = VDAMarshaller.marshal(msg);
        LOG.info("VDA 4905 marshalled (fixed-width):\n{}", edi);

        // Marshal — readable with pipe separators (for debugging)
        String readable = VDAMarshaller.marshalReadable(msg, "|");
        LOG.info("VDA 4905 marshalled (readable):\n{}", readable);

        Assert.assertNotNull(edi);
        Assert.assertTrue("Should contain 511 header", edi.contains("511"));
        Assert.assertTrue("Should contain 512 material", edi.contains("512"));
        Assert.assertTrue("Should contain 513 call", edi.contains("513"));
        Assert.assertTrue("Should contain 519 trailer", edi.contains("519"));
        Assert.assertTrue("Should contain customer number", edi.contains("CUST00001"));
        // Readable output should contain pipe separators
        Assert.assertTrue("Readable should contain pipe separator", readable.contains("|"));
        Assert.assertTrue("Readable should contain record type", readable.contains("511"));
        Assert.assertTrue("Readable should contain customer number", readable.contains("CUST00001"));

        // Round-trip: unmarshal the generated EDI
        VDAUnmarshalResult<VDA4905Message> result = VDAUnmarshaller.unmarshal(
                VDA4905Message.class, new StringReader(edi));
        Assert.assertTrue("Round-trip should parse", result.isParsed());
        Assert.assertEquals("511", result.getData().getHeader().getRecordType());
        Assert.assertEquals("CUST00001", result.getData().getHeader().getCustomerNumber());
        Assert.assertEquals(1, result.getData().getMaterials().size());
        Assert.assertEquals("BRAKE-PAD-001", result.getData().getMaterials().get(0).getBuyerPartNumber().trim());
        Assert.assertEquals("519", result.getData().getTrailer().getRecordType());

        LOG.info("VDA 4905 marshal + round-trip PASSED\n");
    }

    @Test
    public void testVDA4984_Marshal() {
        LOG.info("=== TEST: VDA 4984 — Marshal ===");

        VDA4984Message msg = new VDA4984Message();

        DispatchHeader hdr = new DispatchHeader();
        hdr.setRecordType("721");
        hdr.setVersion("01");
        hdr.setCustomerNumber("CUST00002");
        hdr.setSupplierNumber("SUPP00002");
        hdr.setDeliveryNoteNumber("DN001234");
        hdr.setDeliveryNoteDate("260115");
        hdr.setShippingDate("260116");
        hdr.setArrivalDate("260117");
        msg.setHeader(hdr);

        DispatchItem item = new DispatchItem();
        item.setRecordType("722");
        item.setBuyerPartNumber("GEAR-SHAFT-001");
        item.setSupplierPartNumber("GS-4711");
        item.setDispatchedQuantity("000000005000");
        item.setUnitOfMeasure("PCS");
        item.setDeliveryPoint("PLANT1");
        item.setOrderNumber("CALL000001");
        item.setCumulativeQuantity("000000025000");
        msg.setItems(Arrays.asList(item));

        msg.setPackaging(new ArrayList<>());

        DispatchTrailer trl = new DispatchTrailer();
        trl.setRecordType("729");
        trl.setCustomerNumber("CUST00002");
        trl.setSupplierNumber("SUPP00002");
        trl.setDeliveryNoteNumber("DN001234");
        trl.setDataRecordCount("0000003");
        trl.setTotalPackages("00000");
        msg.setTrailer(trl);

        String edi = VDAMarshaller.marshal(msg);
        LOG.info("VDA 4984 marshalled (fixed-width):\n{}", edi);

        // Readable with separators
        String readable = VDAMarshaller.marshalReadable(msg, "|");
        LOG.info("VDA 4984 marshalled (readable):\n{}", readable);

        Assert.assertNotNull(edi);
        Assert.assertTrue(edi.contains("721"));
        Assert.assertTrue(edi.contains("722"));
        Assert.assertTrue(edi.contains("729"));
        Assert.assertTrue(edi.contains("DN001234"));

        // Round-trip
        VDAUnmarshalResult<VDA4984Message> result = VDAUnmarshaller.unmarshal(
                VDA4984Message.class, new StringReader(edi));
        Assert.assertTrue(result.isParsed());
        Assert.assertEquals("DN001234", result.getData().getHeader().getDeliveryNoteNumber());
        Assert.assertEquals(1, result.getData().getItems().size());
        Assert.assertEquals("PCS", result.getData().getItems().get(0).getUnitOfMeasure().trim());

        LOG.info("VDA 4984 marshal + round-trip PASSED\n");
    }

    @Test
    public void testDelforD96A_Marshal() {
        LOG.info("=== TEST: EDIFACT DELFOR D96A — Marshal ===");

        // Build DELFOR D96A message programmatically
        DelforD96A msg = new DelforD96A();

        InterchangeHeader unb = new InterchangeHeader();
        unb.setSyntaxIdentifier("UNOC:3");
        unb.setInterchangeSender("BUYER001:14");
        unb.setInterchangeRecipient("SUPPLIER001:14");
        unb.setDateTimeOfPreparation("260115:1430");
        unb.setInterchangeControlReference("00000001");
        msg.setInterchangeHeader(unb);

        MessageHeader unh = new MessageHeader();
        unh.setMessageReferenceNumber("1");
        unh.setMessageIdentifier("DELFOR:D:96A:UN");
        msg.setMessageHeader(unh);

        BeginningOfMessage bgm = new BeginningOfMessage();
        bgm.setDocumentNameCode("241");
        bgm.setDocumentNumber("DELFOR-MARSHAL-001");
        bgm.setMessageFunctionCode("9");
        msg.setBeginningOfMessage(bgm);

        DateTimePeriod dtm1 = new DateTimePeriod();
        dtm1.setDateTimePeriodQualifier("137");
        dtm1.setDateTimePeriodValue("20260115");
        dtm1.setDateTimePeriodFormatQualifier("102");

        DateTimePeriod dtm2 = new DateTimePeriod();
        dtm2.setDateTimePeriodQualifier("2");
        dtm2.setDateTimePeriodValue("20260115");
        dtm2.setDateTimePeriodFormatQualifier("102");
        msg.setDateTimePeriods(Arrays.asList(dtm1, dtm2));

        NameAndAddress nadBY = new NameAndAddress();
        nadBY.setPartyQualifier("BY");
        nadBY.setPartyIdIdentification("BUYER001");
        nadBY.setCodeListResponsibleAgency("92");

        NameAndAddress nadSU = new NameAndAddress();
        nadSU.setPartyQualifier("SU");
        nadSU.setPartyIdIdentification("SUPPLIER001");
        nadSU.setCodeListResponsibleAgency("92");
        msg.setNameAndAddresses(Arrays.asList(nadBY, nadSU));

        LineItem lin = new LineItem();
        lin.setLineItemNumber("1");
        lin.setItemNumber("BRAKE-PAD-007");
        lin.setItemNumberType("IN");

        Quantity qty = new Quantity();
        qty.setQuantityQualifier("1");
        qty.setQuantity("2500");
        qty.setMeasureUnitQualifier("PCE");

        LineItemGroup lig = new LineItemGroup();
        lig.setLineItem(lin);
        lig.setQuantities(Arrays.asList(qty));
        msg.setLineItemGroups(Arrays.asList(lig));

        // Marshal
        String edi = EDIFACTMarshaller.marshal(msg);
        LOG.info("DELFOR D96A marshalled:\n{}", edi);

        Assert.assertNotNull(edi);
        Assert.assertTrue("Should start with UNA", edi.startsWith("UNA"));
        Assert.assertTrue("Should contain UNB segment", edi.contains("UNB+"));
        Assert.assertTrue("Should contain UNH segment", edi.contains("UNH+"));
        Assert.assertTrue("Should contain BGM segment", edi.contains("BGM+241"));
        Assert.assertTrue("Should contain DTM segment", edi.contains("DTM+137:20260115:102"));
        Assert.assertTrue("Should contain NAD+BY", edi.contains("NAD+BY+BUYER001"));
        Assert.assertTrue("Should contain NAD+SU", edi.contains("NAD+SU+SUPPLIER001"));
        Assert.assertTrue("Should contain LIN segment", edi.contains("LIN+1"));
        Assert.assertTrue("Should contain QTY segment", edi.contains("QTY+1:2500:PCE"));

        // Round-trip: unmarshal the generated EDI
        EDIFACTUnmarshalResult<DelforD96A> result = EDIFACTUnmarshaller.unmarshal(
                DelforD96A.class, new StringReader(edi));
        Assert.assertTrue("Round-trip should parse", result.isParsed());
        DelforD96A rt = result.getData();

        Assert.assertEquals("241", rt.getBeginningOfMessage().getDocumentNameCode());
        Assert.assertEquals("DELFOR-MARSHAL-001", rt.getBeginningOfMessage().getDocumentNumber());
        Assert.assertEquals("9", rt.getBeginningOfMessage().getMessageFunctionCode());
        Assert.assertEquals("137", rt.getDateTimePeriods().get(0).getDateTimePeriodQualifier());
        Assert.assertEquals("20260115", rt.getDateTimePeriods().get(0).getDateTimePeriodValue());
        Assert.assertEquals("102", rt.getDateTimePeriods().get(0).getDateTimePeriodFormatQualifier());
        Assert.assertEquals("BY", rt.getNameAndAddresses().get(0).getPartyQualifier());
        Assert.assertEquals("BUYER001", rt.getNameAndAddresses().get(0).getPartyIdIdentification());
        Assert.assertEquals("1", rt.getQuantities().get(0).getQuantityQualifier());
        Assert.assertEquals("2500", rt.getQuantities().get(0).getQuantity());
        Assert.assertEquals("PCE", rt.getQuantities().get(0).getMeasureUnitQualifier());

        LOG.info("DELFOR D96A marshal + round-trip PASSED\n");
    }

    @Test
    public void testDelforD04A_Marshal() {
        LOG.info("=== TEST: EDIFACT DELFOR D04A — Marshal ===");

        // Build DELFOR D04A message programmatically
        // D04A shares the same segment classes as D96A (same package hierarchy)
        DelforD04A msg = new DelforD04A();

        // Use the D04A segment classes (same structure as D96A for basic segments)
        javax.edi.model.edifact.delfor.d96a.segment.InterchangeHeader unb =
                new javax.edi.model.edifact.delfor.d96a.segment.InterchangeHeader();
        unb.setSyntaxIdentifier("UNOC:3");
        unb.setInterchangeSender("VWAG:ZZZ");
        unb.setInterchangeRecipient("BOSCH:ZZZ");
        unb.setDateTimeOfPreparation("260115:1530");
        unb.setInterchangeControlReference("D04A-CTRL-001");
        msg.setInterchangeHeader(unb);

        javax.edi.model.edifact.delfor.d96a.segment.MessageHeader unh =
                new javax.edi.model.edifact.delfor.d96a.segment.MessageHeader();
        unh.setMessageReferenceNumber("1");
        unh.setMessageIdentifier("DELFOR:D:04A:UN");
        msg.setMessageHeader(unh);

        javax.edi.model.edifact.delfor.d96a.segment.BeginningOfMessage bgm =
                new javax.edi.model.edifact.delfor.d96a.segment.BeginningOfMessage();
        bgm.setDocumentNameCode("241");
        bgm.setDocumentNumber("D04A-DOC-001");
        bgm.setMessageFunctionCode("9");
        msg.setBeginningOfMessage(bgm);

        javax.edi.model.edifact.delfor.d96a.segment.DateTimePeriod dtm =
                new javax.edi.model.edifact.delfor.d96a.segment.DateTimePeriod();
        dtm.setDateTimePeriodQualifier("137");
        dtm.setDateTimePeriodValue("20260115");
        dtm.setDateTimePeriodFormatQualifier("102");
        msg.setDateTimePeriods(Arrays.asList(dtm));

        javax.edi.model.edifact.delfor.d96a.segment.NameAndAddress nad =
                new javax.edi.model.edifact.delfor.d96a.segment.NameAndAddress();
        nad.setPartyQualifier("BY");
        nad.setPartyIdIdentification("VWAG");
        nad.setCodeListResponsibleAgency("92");
        msg.setNameAndAddresses(Arrays.asList(nad));

        javax.edi.model.edifact.delfor.d96a.segment.LineItem lin =
                new javax.edi.model.edifact.delfor.d96a.segment.LineItem();
        lin.setLineItemNumber("1");
        lin.setItemNumber("ABS-SENSOR-L");
        lin.setItemNumberType("IN");
        msg.setLineItems(Arrays.asList(lin));

        javax.edi.model.edifact.delfor.d96a.segment.Quantity qty =
                new javax.edi.model.edifact.delfor.d96a.segment.Quantity();
        qty.setQuantityQualifier("1");
        qty.setQuantity("3000");
        qty.setMeasureUnitQualifier("PCE");
        msg.setQuantities(Arrays.asList(qty));

        // Marshal
        String edi = EDIFACTMarshaller.marshal(msg);
        LOG.info("DELFOR D04A marshalled:\n{}", edi);

        Assert.assertNotNull(edi);
        Assert.assertTrue("Should start with UNA", edi.startsWith("UNA"));
        Assert.assertTrue("Should contain BGM+241", edi.contains("BGM+241"));
        Assert.assertTrue("Should contain QTY+1:3000:PCE", edi.contains("QTY+1:3000:PCE"));
        Assert.assertTrue("Should contain NAD+BY+VWAG", edi.contains("NAD+BY+VWAG"));

        // Round-trip
        EDIFACTUnmarshalResult<DelforD04A> result = EDIFACTUnmarshaller.unmarshal(
                DelforD04A.class, new StringReader(edi));
        Assert.assertTrue("Round-trip should parse", result.isParsed());
        Assert.assertEquals("241", result.getData().getBeginningOfMessage().getDocumentNameCode());
        Assert.assertEquals("D04A-DOC-001", result.getData().getBeginningOfMessage().getDocumentNumber());
        Assert.assertEquals("1", result.getData().getQuantities().get(0).getQuantityQualifier());
        Assert.assertEquals("3000", result.getData().getQuantities().get(0).getQuantity());
        Assert.assertEquals("PCE", result.getData().getQuantities().get(0).getMeasureUnitQualifier());

        LOG.info("DELFOR D04A marshal + round-trip PASSED\n");
    }

    @Test
    public void testDelforD96A_RealWorld_RoundTrip() {
        LOG.info("=== TEST: DELFOR D96A — Real-world Unmarshal then Marshal Round-trip ===");

        // Step 1: Unmarshal the real-world file
        InputStreamReader reader = new InputStreamReader(
                this.getClass().getClassLoader().getResourceAsStream("DELFORD96A.edi"));
        EDIFACTUnmarshalResult<DelforD96A> result = EDIFACTUnmarshaller.unmarshal(
                DelforD96A.class, reader);
        Assert.assertTrue(result.isParsed());
        DelforD96A original = result.getData();

        // Step 2: Marshal the parsed object back to EDI
        String edi = EDIFACTMarshaller.marshal(original);
        LOG.info("Re-marshalled EDI (first 500 chars):\n{}", edi.substring(0, Math.min(500, edi.length())));

        Assert.assertTrue("Should contain UNA", edi.startsWith("UNA"));
        Assert.assertTrue("Should contain BGM+241", edi.contains("BGM+241"));
        Assert.assertTrue("Should contain QTY+70:375:PCE", edi.contains("QTY+70:375:PCE"));

        // Step 3: Unmarshal again and verify key fields match
        EDIFACTUnmarshalResult<DelforD96A> result2 = EDIFACTUnmarshaller.unmarshal(
                DelforD96A.class, new StringReader(edi));
        Assert.assertTrue(result2.isParsed());
        DelforD96A roundTrip = result2.getData();

        Assert.assertEquals(original.getBeginningOfMessage().getDocumentNameCode(),
                roundTrip.getBeginningOfMessage().getDocumentNameCode());
        Assert.assertEquals(original.getBeginningOfMessage().getDocumentNumber(),
                roundTrip.getBeginningOfMessage().getDocumentNumber());
        Assert.assertEquals(original.getQuantities().get(0).getQuantityQualifier(),
                roundTrip.getQuantities().get(0).getQuantityQualifier());
        Assert.assertEquals(original.getQuantities().get(0).getQuantity(),
                roundTrip.getQuantities().get(0).getQuantity());
        Assert.assertEquals(original.getQuantities().get(0).getMeasureUnitQualifier(),
                roundTrip.getQuantities().get(0).getMeasureUnitQualifier());

        LOG.info("Real-world DELFOR D96A round-trip PASSED\n");
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private static String vdaField(String value, int width) {
        if (value == null) value = "";
        if (value.length() >= width) return value.substring(0, width);
        return String.format("%-" + width + "s", value);
    }

    private static String padRecord(String record, int length) {
        if (record.length() >= length) return record.substring(0, length);
        return String.format("%-" + length + "s", record);
    }
}

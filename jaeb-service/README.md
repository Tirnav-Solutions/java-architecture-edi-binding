# JAEB EDI Middleware Service

Standalone REST microservice that exposes EDI parsing, generation, validation, 997 acknowledgement generation, schema generation, sample generation, and documentation generation as HTTP endpoints.

Other systems can `POST` raw EDI or JSON and get results back **without any EDI library dependency**.

## Quick Start

```bash
# Build
cd java-architecture-edi-binding
mvn clean package -pl jaeb-service -am -DskipTests -Dmaven.test.skip=true

# Run
java -jar jaeb-service/target/jaeb-service-0.0.1-SNAPSHOT.jar

# Or with Maven
mvn spring-boot:run -pl jaeb-service
```

Service starts on **http://localhost:8080**

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/api/edi/health
- API Info: http://localhost:8080/api/edi/info

## Docker

```bash
docker build -t jaeb-edi-service -f jaeb-service/Dockerfile .
docker run -p 8080:8080 jaeb-edi-service
```

## API Endpoints

### Core Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/edi/parse` | Parse raw EDI text into JSON |
| `POST` | `/api/edi/generate` | Generate raw EDI from JSON payload |
| `POST` | `/api/edi/validate` | Validate raw EDI or JSON payload |
| `POST` | `/api/edi/acknowledge` | **Parse EDI and auto-generate 997 Functional Acknowledgement** |

### Schema & Documentation

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/edi/schema/{type}` | JSON Schema for a transaction type |
| `GET` | `/api/edi/sample/{type}` | Sample JSON template |
| `GET` | `/api/edi/doc/{type}` | EDI Implementation Guide (TEXT or MARKDOWN) |
| `GET` | `/api/edi/schema/{type}/raw` | Raw JSON Schema (text/plain) |
| `GET` | `/api/edi/sample/{type}/raw` | Raw sample JSON (text/plain) |
| `GET` | `/api/edi/doc/{type}/raw` | Raw implementation guide (text/plain) |

### Service Info

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/edi/types` | List all supported transaction types |
| `GET` | `/api/edi/health` | Health check |
| `GET` | `/api/edi/info` | API summary with all endpoints |

## Supported Transaction Types

| Code | Type |
|------|------|
| 810 | Invoice |
| 832 | Price/Sales Catalog |
| 846 | Inventory Inquiry |
| 850 | Purchase Order |
| 855 | PO Acknowledgement |
| 856 | Advance Shipment Notice |
| 997 | Functional Acknowledgement |

Use either the code (`850`) or the class name (`PurchaseOrder`) in API calls.

## Example: Parse EDI and Generate 997 Acknowledgement

This is the primary middleware use case. One API call does everything:

```bash
curl -X POST http://localhost:8080/api/edi/acknowledge \
  -H "Content-Type: application/json" \
  -d '{
    "transactionType": "850",
    "ediContent": "ISA*00*          *00*          *ZZ*SENDER         *ZZ*RECEIVER       *260227*1200*U*00401*000000001*0*P*>~GS*PO*SENDERAPP*RECEIVERAPP*20260227*1200*1*X*004010~ST*850*0001~BEG*00*NE*PO123**20260227~SE*3*0001~GE*1*1~IEA*1*000000001~"
  }'
```

Response:
```json
{
  "success": true,
  "message": "EDI parsed and 997 acknowledgement generated (AK9=A)",
  "data": {
    "parseResult": {
      "parsed": true,
      "valid": true,
      "segmentCount": 7
    },
    "parsedData": { ... },
    "acknowledgement": {
      "success": true,
      "acknowledgeCode": "A",
      "controlNumber": "123456789",
      "edi997Output": "ISA*00*...*~GS*FA*...*~ST*997*...*~AK1*PO*1*~AK2*850*0001*~AK5*A*~AK9*A*1*1*1*~SE*6*...*~GE*1*...*~IEA*1*...*~"
    }
  }
}
```

The 997 automatically:
- Swaps sender/receiver from the original ISA/GS envelopes
- Sets GS01 = "FA" (Functional Acknowledgement)
- Sets AK9 acknowledge code based on validation status (A=Accepted, E=Errors, R=Rejected)
- Includes AK2/AK5 pairs for each transaction set in the source

## Example: Parse Raw EDI

```bash
curl -X POST http://localhost:8080/api/edi/parse \
  -H "Content-Type: application/json" \
  -d '{
    "transactionType": "850",
    "ediContent": "ISA*00*...*~GS*PO*...*~..."
  }'
```

## Example: Generate EDI from JSON

```bash
curl -X POST http://localhost:8080/api/edi/generate \
  -H "Content-Type: application/json" \
  -d '{
    "transactionType": "850",
    "payload": { ... }
  }'
```

## Example: Validate

```bash
# Validate raw EDI
curl -X POST http://localhost:8080/api/edi/validate \
  -H "Content-Type: application/json" \
  -d '{
    "transactionType": "850",
    "ediContent": "ISA*00*...*~..."
  }'

# Validate JSON payload
curl -X POST http://localhost:8080/api/edi/validate \
  -H "Content-Type: application/json" \
  -d '{
    "transactionType": "850",
    "payload": { ... }
  }'
```

## Example: Get Schema/Sample/Doc

```bash
# JSON Schema
curl http://localhost:8080/api/edi/schema/850

# Sample JSON
curl http://localhost:8080/api/edi/sample/850

# Implementation Guide (text)
curl http://localhost:8080/api/edi/doc/850

# Implementation Guide (markdown)
curl "http://localhost:8080/api/edi/doc/850?format=MARKDOWN"

# Raw outputs (no JSON wrapper)
curl http://localhost:8080/api/edi/schema/850/raw
curl http://localhost:8080/api/edi/doc/850/raw
```

## Using EDI997Generator Directly (Library Usage)

If you don't want to use the REST service, you can use `EDI997Generator` directly in your code:

```java
// After unmarshalling any EDI message
EDIUnmarshalResult<PurchaseOrder> result = EDIUnmarshaller.unmarshalResult(PurchaseOrder.class, reader);

// Generate 997 acknowledgement
EDI997Generator.Result ack = EDI997Generator.generate(result);

if (ack.isSuccess()) {
    String edi997 = ack.getEdiOutput();           // raw EDI 997 string
    String controlNum = ack.getControlNumber();    // for tracking
    String ackCode = ack.getAcknowledgeCode();     // A, E, P, or R
    // Send edi997 back to trading partner...
}

// Or from a parsed object directly (assumes full acceptance)
EDI997Generator.Result ack = EDI997Generator.generate(purchaseOrder);

// Or with explicit acknowledge codes
EDI997Generator.Result ack = EDI997Generator.generate(
    purchaseOrder, 
    EDI997Generator.ACCEPTED_WITH_ERRORS,   // AK9 group code
    EDI997Generator.TS_ACCEPTED_WITH_ERRORS, // AK5 txn set code
    "000012345"                              // control number
);
```

## Architecture

```
jaeb-core          -- Core library: annotations, marshaller, unmarshaller, 
                      validation, EDI997Generator, schema/doc generators
jaeb-model-x12     -- X12 transaction set models (850, 810, 856, etc.)
jaeb-service       -- Spring Boot REST microservice (this module)
```

The service is stateless and can be horizontally scaled. All operations are synchronous request/response.

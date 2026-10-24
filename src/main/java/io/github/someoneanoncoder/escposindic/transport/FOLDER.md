# transport

**Path:** src/main/java/io/github/someoneanoncoder/escposindic/transport
**Purpose:** Delivers a finished byte stream to a printer queue, a network printer or a file.

## Contents
| File | Purpose | Plan |
|---|---|---|
| Transport.java | One-method contract and the 8 MiB cap | L3-05 |
| PrintQueueTransport.java | Windows (and other) print queue via javax.print raw bytes | L3-05 |
| TcpTransport.java | Raw TCP (port 9100) with connect and write deadlines | L3-05 |
| FileSinkTransport.java | File sink for tests and diagnostics | L3-05 |
| TransportChecks.java | Shared size check (package-private) | L3-05 |
| package-info.java | Package documentation | L3-05 |

## Rules for adding files here
- Stateless between jobs; every wait has a timeout; every input is validated.

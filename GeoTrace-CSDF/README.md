# GeoTrace — CSDF Mini Project

**Topic:** Enhancing IP Address Geocoding, Geolocating and Visualization for Digital Forensics

A Java 21 + Spring Boot demo with IP metadata lookup, Leaflet map visualization, CSV log analysis, repeated-IP frequency counts, chronological event sorting, and SHA-256 file hashing.

## Requirements
- JDK 21
- Maven 3.9+
- Internet access for geolocation API calls and map tiles

## Run
From this directory:

```bash
mvn spring-boot:run
```

Open http://localhost:8080

Check backend health at http://localhost:8080/api/health

## Test the application
1. Open the website.
2. Look up a public IP such as `8.8.8.8`.
3. Click **Download sample CSV**.
4. Upload `sample-logs.csv` and inspect counts, event order, and the SHA-256 hash.
5. Export the parsed event results to CSV.

## CSV format
Required header (any one): `ip`, `ip_address`, `source_ip`, `src_ip`

Optional timestamp headers: `timestamp`, `time`, `datetime`, `date`

Optional event headers: `event`, `action`, `message`, `description`

Maximum upload size: 5 MB. The analyzer keeps uploaded content in memory and does not persist the original file.

## API endpoints
- `GET /api/health`
- `GET /api/lookup?ip=8.8.8.8`
- `POST /api/analyze` with multipart field named `file`

## Notes and limitations
- `ipapi.co` is an external provider; service limits, availability, and terms apply.
- IP geolocation is approximate and cannot prove the identity or exact physical location of a person.
- Private, loopback, link-local, multicast, and several special-use IPv4 ranges are blocked from provider lookup. Special-purpose IP allocations are complex; this demo is not a complete IANA registry implementation.
- SHA-256 identifies the uploaded file's bytes at analysis time; it does not independently prove evidence authenticity or chain of custody.
- CSV parsing supports quoted fields on a single line but is intentionally lightweight, not a complete RFC 4180 parser.
- This is a student demo, not a production forensic evidence-management system.
- OpenStreetMap tiles require attribution and must be used according to the tile usage policy.

## Suggested 2–3 day plan
Day 1: run app, understand REST endpoints, validate IPs and test provider responses.
Day 2: test CSV analysis, map markers, frequency aggregation and SHA-256.
Day 3: UI polish, test cases, screenshots, report and viva preparation.

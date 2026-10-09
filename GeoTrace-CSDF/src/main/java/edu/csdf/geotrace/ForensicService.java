package edu.csdf.geotrace;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

@Service
public class ForensicService {
    public Map<String, Object> analyze(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Choose a non-empty CSV file.");
        if (file.getSize() > 5 * 1024 * 1024) throw new IllegalArgumentException("Maximum upload size is 5 MB.");

        byte[] original = file.getBytes();
        String sha256 = sha256(original);
        List<Map<String, Object>> events = new ArrayList<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        int invalid = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new java.io.ByteArrayInputStream(original), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) throw new IllegalArgumentException("CSV file is empty.");
            List<String> headers = parseCsvLine(headerLine);
            int ipIndex = findHeader(headers, "ip", "ip_address", "source_ip", "src_ip");
            int timeIndex = findHeader(headers, "timestamp", "time", "datetime", "date");
            int eventIndex = findHeader(headers, "event", "action", "message", "description");
            if (ipIndex < 0) throw new IllegalArgumentException("CSV needs an IP column: ip, ip_address, source_ip or src_ip.");

            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) continue;
                List<String> row = parseCsvLine(line);
                if (ipIndex >= row.size()) { invalid++; continue; }
                String rawIp = row.get(ipIndex).trim();
                try {
                    String ip = IpAddressUtil.normalizeAndValidate(rawIp);
                    counts.merge(ip, 1, Integer::sum);
                    Map<String, Object> event = new LinkedHashMap<>();
                    event.put("row", rowNumber);
                    event.put("ip", ip);
                    event.put("timestamp", getCell(row, timeIndex));
                    event.put("event", getCell(row, eventIndex));
                    events.add(event);
                } catch (IllegalArgumentException ex) {
                    invalid++;
                }
                if (events.size() >= 10000) break;
            }
        }

        events.sort(Comparator.comparing(e -> {
            Object ts = e.get("timestamp");
            return ts == null ? "" : ts.toString();
        }));

        List<Map<String, Object>> frequencies = counts.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .map(e -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("ip", e.getKey());
                item.put("count", e.getValue());
                return item;
            }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fileName", file.getOriginalFilename() == null ? "uploaded.csv" : file.getOriginalFilename());
        result.put("fileSizeBytes", original.length);
        result.put("sha256", sha256);
        result.put("processedAt", Instant.now().toString());
        result.put("totalValidEvents", events.size());
        result.put("uniqueIpCount", counts.size());
        result.put("invalidRows", invalid);
        result.put("topIps", frequencies);
        result.put("events", events);
        result.put("note", "SHA-256 records the uploaded file's hash at analysis time; it does not independently prove evidence authenticity.");
        return result;
    }

    private int findHeader(List<String> headers, String... candidates) {
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i).trim().replace("\uFEFF", "").toLowerCase(Locale.ROOT);
            for (String candidate : candidates) if (header.equals(candidate)) return i;
        }
        return -1;
    }

    private String getCell(List<String> row, int index) {
        return index >= 0 && index < row.size() && !row.get(index).isBlank() ? row.get(index).trim() : "";
    }

    // Small CSV line parser supporting quoted fields and escaped double quotes.
    private List<String> parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cell.append('"'); i++;
                } else quoted = !quoted;
            } else if (c == ',' && !quoted) {
                cells.add(cell.toString()); cell.setLength(0);
            } else cell.append(c);
        }
        cells.add(cell.toString());
        return cells;
    }

    private String sha256(byte[] bytes) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder out = new StringBuilder();
            for (byte b : hash) out.append(String.format("%02x", b));
            return out.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available.", ex);
        }
    }
}

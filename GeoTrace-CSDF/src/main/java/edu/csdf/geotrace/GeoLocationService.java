package edu.csdf.geotrace;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class GeoLocationService {
    private final RestClient client = RestClient.builder()
        .baseUrl("https://ipapi.co")
        .defaultHeader("User-Agent", "GeoTrace-CSDF-MiniProject/1.0")
        .build();

    public Map<String, Object> lookup(String rawIp) {
        String ip = IpAddressUtil.normalizeAndValidate(rawIp);
        if (!IpAddressUtil.isPublicCandidate(ip)) {
            throw new IllegalArgumentException("Private, loopback, link-local, multicast or reserved-range addresses are not looked up.");
        }

        try {
            JsonNode data = client.get()
                .uri("/" + UriUtils.encodePathSegment(ip, StandardCharsets.UTF_8) + "/json/")
                .retrieve()
                .body(JsonNode.class);

            if (data == null || data.path("error").asBoolean(false)) {
                String reason = data == null ? "No response from geolocation provider."
                    : data.path("reason").asText("Geolocation lookup failed.");
                throw new IllegalStateException(reason);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("ip", data.path("ip").asText(ip));
            result.put("city", textOrNull(data, "city"));
            result.put("region", textOrNull(data, "region"));
            result.put("country", textOrNull(data, "country_name"));
            result.put("countryCode", textOrNull(data, "country_code"));
            result.put("latitude", numberOrNull(data, "latitude"));
            result.put("longitude", numberOrNull(data, "longitude"));
            result.put("timezone", textOrNull(data, "timezone"));
            result.put("asn", textOrNull(data, "asn"));
            result.put("organization", textOrNull(data, "org"));
            result.put("source", "ipapi.co");
            result.put("note", "IP geolocation is approximate and does not identify a person's exact location.");
            return result;
        } catch (RestClientException ex) {
            throw new IllegalStateException("Geolocation provider unavailable or request limit reached. Try again later.", ex);
        }
    }

    private String textOrNull(JsonNode node, String key) {
        JsonNode value = node.get(key);
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    private Object numberOrNull(JsonNode node, String key) {
        JsonNode value = node.get(key);
        return value == null || value.isNull() || !value.isNumber() ? null : value.numberValue();
    }
}

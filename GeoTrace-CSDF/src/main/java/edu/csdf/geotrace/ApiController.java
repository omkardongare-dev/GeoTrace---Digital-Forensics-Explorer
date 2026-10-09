package edu.csdf.geotrace;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final GeoLocationService geoLocationService;
    private final ForensicService forensicService;

    public ApiController(GeoLocationService geoLocationService, ForensicService forensicService) {
        this.geoLocationService = geoLocationService;
        this.forensicService = forensicService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "application", "GeoTrace CSDF");
    }

    @GetMapping("/lookup")
    public ResponseEntity<?> lookup(@RequestParam String ip) {
        try {
            return ResponseEntity.ok(geoLocationService.lookup(ip));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyze(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(forensicService.analyze(file));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Unable to read uploaded file."));
        }
    }
}

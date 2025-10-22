package com.example.fccrawler.controller;

import com.example.fccrawler.service.CrawlerService;
import com.example.fccrawler.model.RedeemCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
public class CrawlController {

    @Autowired
    private CrawlerService crawlerService;

    /**
     * Endpoint chính: crawl code ẩn + thông tin đầy đủ
     * 
     * Query params:
     *   - url: trang web cần crawl (optional)
     *   - detailed: true trả RedeemCode (code+reward+date), false trả chỉ codes (default: true)
     * 
     * Examples:
     *   GET /api/crawl
     *   GET /api/crawl?detailed=false
     *   GET /api/crawl?url=https://example.com&detailed=true
     */
    @GetMapping("/api/crawl")
    public ResponseEntity<?> crawl(
            @RequestParam(required = false) String url,
            @RequestParam(required = false, defaultValue = "true") boolean detailed) {
        
        String defaultUrl = "https://www.fcmobileforum.com/fcmobile-redeem-codes";
        if (url == null || url.isBlank()) {
            url = defaultUrl;
        }

        try {
            List<RedeemCode> codesWithDetails = crawlerService.fetchCodes(url);
            
            if (detailed) {
                // Trả về List<RedeemCode> - đầy đủ: code + reward + date + status
                return ResponseEntity.ok(codesWithDetails);
            } else {
                // Trả về List<String> - chỉ codes
                List<String> onlyCodes = codesWithDetails.stream()
                        .map(RedeemCode::getCode)
                        .collect(Collectors.toList());
                return ResponseEntity.ok(onlyCodes);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Error: " + e.getMessage());
        }
    }

    /**
     * Endpoint riêng: luôn trả structured data (code + reward + date)
     */
    @GetMapping("/api/crawl/full")
    public ResponseEntity<List<RedeemCode>> getCrawlFull(
            @RequestParam(required = false) String url) {
        
        String defaultUrl = "https://www.fcmobileforum.com/fcmobile-redeem-codes";
        if (url == null || url.isBlank()) {
            url = defaultUrl;
        }

        try {
            return ResponseEntity.ok(crawlerService.fetchCodes(url));
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Endpoint riêng: luôn trả chỉ codes
     */
    @GetMapping("/api/crawl/codes")
    public ResponseEntity<List<String>> getCrawlCodesOnly(
            @RequestParam(required = false) String url) {
        
        String defaultUrl = "https://www.fcmobileforum.com/fcmobile-redeem-codes";
        if (url == null || url.isBlank()) {
            url = defaultUrl;
        }

        try {
            List<RedeemCode> codesWithDetails = crawlerService.fetchCodes(url);
            List<String> onlyCodes = codesWithDetails.stream()
                    .map(RedeemCode::getCode)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(onlyCodes);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
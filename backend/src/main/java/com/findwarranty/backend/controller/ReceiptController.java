package com.findwarranty.backend.controller;

import com.findwarranty.backend.model.Receipt;
import com.findwarranty.backend.model.User;
import com.findwarranty.backend.repository.ReceiptRepository;
import com.findwarranty.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

    @Autowired
    private ReceiptRepository receiptRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserReceipts(@PathVariable Long userId) {
        try {
            List<Receipt> receipts = receiptRepository.findByUserId(userId);
            return ResponseEntity.ok(receipts);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error fetching receipts: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createReceipt(
            @RequestParam("storeName") String storeName,
            @RequestParam("productName") String productName,
            @RequestParam("purchaseDate") String purchaseDateStr,
            @RequestParam("price") BigDecimal price,
            @RequestParam("category") String category,
            @RequestParam("warrantyDuration") String warrantyDuration,
            @RequestParam("userId") Long userId,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Receipt receipt = new Receipt();
            receipt.setStoreName(storeName);
            receipt.setProductName(productName);
            
            LocalDate purchaseDate = LocalDate.parse(purchaseDateStr);
            receipt.setPurchaseDate(purchaseDate);
            receipt.setPrice(price);
            receipt.setCategory(category);
            receipt.setWarrantyDuration(warrantyDuration);
            receipt.setUser(user);

            // Calculate expiry
            LocalDate expiryDate = calculateExpiry(purchaseDate, warrantyDuration);
            receipt.setWarrantyExpiryDate(expiryDate);

            if (file != null && !file.isEmpty()) {
                receipt.setPdfData(file.getBytes());
            }

            Receipt saved = receiptRepository.save(receipt);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error creating receipt: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReceipt(@PathVariable Long id) {
        receiptRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateReceipt(
            @PathVariable Long id,
            @RequestParam("storeName") String storeName,
            @RequestParam("productName") String productName,
            @RequestParam("purchaseDate") String purchaseDateStr,
            @RequestParam("price") BigDecimal price,
            @RequestParam("category") String category,
            @RequestParam("warrantyDuration") String warrantyDuration,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        try {
            Receipt receipt = receiptRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Receipt not found"));

            receipt.setStoreName(storeName);
            receipt.setProductName(productName);
            
            LocalDate purchaseDate = LocalDate.parse(purchaseDateStr);
            receipt.setPurchaseDate(purchaseDate);
            receipt.setPrice(price);
            receipt.setCategory(category);
            receipt.setWarrantyDuration(warrantyDuration);

            // Recalculate expiry
            LocalDate expiryDate = calculateExpiry(purchaseDate, warrantyDuration);
            receipt.setWarrantyExpiryDate(expiryDate);

            if (file != null && !file.isEmpty()) {
                receipt.setPdfData(file.getBytes());
            }

            Receipt saved = receiptRepository.save(receipt);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error updating receipt: " + e.getMessage());
        }
    }

    private LocalDate calculateExpiry(LocalDate start, String duration) {
        if (start == null) return null;
        switch (duration.toLowerCase()) {
            case "6 months": return start.plusMonths(6);
            case "1 year": return start.plusYears(1);
            case "2 years": return start.plusYears(2);
            case "3 years": return start.plusYears(3);
            case "5 years": return start.plusYears(5);
            case "lifetime": return start.plusYears(99);
            default: return null; // "Other" or unknown
        }
    }
}

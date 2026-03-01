package com.findwarranty.backend.service;

import com.findwarranty.backend.dto.OcrReceiptDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * Prepoznavanje podataka sa računa (slika ili PDF) pomoću Google Gemini API
 * (najjeftiniji model: gemini-1.5-flash).
 */
@Service
public class GeminiReceiptService {

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    private static final String EXTRACT_PROMPT = """
        Ovo je slika ili PDF računa (receipt). Izvuci sledeće podatke i vrati SAMO validan JSON bez markdown oznaka, bez dodatnog teksta:
        - storeName: naziv radnje/prodavnice (string)
        - productName: naziv proizvoda/artikla (string)
        - purchaseDate: datum kupovine u formatu YYYY-MM-DD (string, npr. 2024-03-15)
        - price: ukupna cena kao broj sa dve decimale, tačka kao decimalni separator (string, npr. "1299.00")

        Ako neki podatak nije vidljiv ili nije na računu, stavi prazan string "" za to polje.
        Odgovori isključivo JSON objektom sa tačno tim poljima: {"storeName":"...","productName":"...","purchaseDate":"...","price":"..."}
        """;

    @Value("${gemini.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OcrReceiptDto extractReceiptData(MultipartFile file) throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            return new OcrReceiptDto("", "", "", "");
        }

        String mimeType = file.getContentType();
        if (mimeType == null) {
            mimeType = file.getOriginalFilename() != null && file.getOriginalFilename().toLowerCase().endsWith(".pdf")
                ? "application/pdf"
                : "image/jpeg";
        }
        String base64 = Base64.getEncoder().encodeToString(file.getBytes());

        Map<String, Object> inlineData = new HashMap<>();
        inlineData.put("mimeType", mimeType);
        inlineData.put("data", base64);

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", EXTRACT_PROMPT);

        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("inlineData", inlineData));
        parts.add(textPart);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(Map.of("parts", parts)));
        requestBody.put("generationConfig", Map.of(
            "responseMimeType", "application/json",
            "temperature", 0.1
        ));

        String url = GEMINI_URL + "?key=" + apiKey;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            return new OcrReceiptDto("", "", "", "");
        }

        Map<?, ?> body = response.getBody();
        return parseGeminiResponse(body);
    }

    @SuppressWarnings("unchecked")
    private OcrReceiptDto parseGeminiResponse(Map<?, ?> body) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return new OcrReceiptDto("", "", "", "");
            }
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            if (content == null) return new OcrReceiptDto("", "", "", "");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) return new OcrReceiptDto("", "", "", "");

            String text = (String) parts.get(0).get("text");
            if (text == null || text.isBlank()) return new OcrReceiptDto("", "", "", "");

            text = text.trim();
            if (text.startsWith("```")) {
                text = text.replaceFirst("^```\\w*\\n?", "").replaceFirst("\\n?```\\s*$", "");
            }
            JsonNode root = objectMapper.readTree(text);
            OcrReceiptDto dto = new OcrReceiptDto();
            dto.setStoreName(getText(root, "storeName"));
            dto.setProductName(getText(root, "productName"));
            dto.setPurchaseDate(getText(root, "purchaseDate"));
            dto.setPrice(getText(root, "price"));
            return dto;
        } catch (Exception e) {
            return new OcrReceiptDto("", "", "", "");
        }
    }

    private static String getText(JsonNode node, String field) {
        JsonNode f = node.get(field);
        return f != null && !f.isNull() ? f.asText("") : "";
    }
}

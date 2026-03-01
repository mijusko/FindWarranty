package com.findwarranty.backend.service;

import com.findwarranty.backend.model.ChatMessage;
import com.findwarranty.backend.model.Receipt;
import com.findwarranty.backend.model.User;
import com.findwarranty.backend.repository.ChatMessageRepository;
import com.findwarranty.backend.repository.ReceiptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    @Autowired
    private ChatMessageRepository messageRepository;

    @Autowired
    private ReceiptRepository receiptRepository;

    @Value("${qwen.api.key:}")
    private String apiKey;

    @Value("${qwen.api.base-url:}")
    private String baseUrl;

    @Value("${qwen.model-name:qwen2.5-32b-instruct}")
    private String modelName;

    private final RestTemplate restTemplate = new RestTemplate();

    public List<ChatMessage> getChatHistory(User user) {
        return messageRepository.findByUserOrderByTimestampAsc(user);
    }

    public void clearHistory(User user) {
        messageRepository.deleteByUser(user);
    }

    public ChatMessage processUserMessage(User user, String content, boolean includeReceipts) {
        // 1. Save user message
        ChatMessage userMsg = new ChatMessage(content, "user", user);
        messageRepository.save(userMsg);

        // 2. Prepare context
        List<Map<String, String>> messages = new ArrayList<>();
        
        // System Prompt
        String systemPrompt = "You are a professional Financial Advisor. " +
                "Your goal is to help users manage their spending and warranties. " +
                "Be concise, helpful, and provide actionable financial advice. " +
                "Answer in the same language as the user's input.";
        messages.add(Map.of("role", "system", "content", systemPrompt));

        // Add receipt context if requested
        if (includeReceipts) {
            List<Receipt> receipts = receiptRepository.findByUserOrderByPurchaseDateDesc(user);
            if (!receipts.isEmpty()) {
                String receiptData = receipts.stream()
                        .map(r -> String.format("- %s: %s, Price: %.2f, Category: %s, Purchased: %s",
                                r.getStoreName(), r.getProductName(), r.getPrice(), r.getCategory(), r.getPurchaseDate()))
                        .collect(Collectors.joining("\n"));
                
                messages.add(Map.of("role", "system", "content", 
                        "User's Receipt Data:\n" + receiptData));
            }
        }

        // Add history
        List<ChatMessage> history = messageRepository.findByUserOrderByTimestampAsc(user);
        for (ChatMessage msg : history) {
            messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
        }

        // 3. Call AI API (OpenAI-compatible)
        String aiResponseContent = callAiApi(messages);

        // 4. Save and return AI message
        ChatMessage aiMsg = new ChatMessage(aiResponseContent, "assistant", user);
        return messageRepository.save(aiMsg);
    }

    private String callAiApi(List<Map<String, String>> messages) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "AI API key not configured. Please add QWEN_API_KEY to your environment.";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("model", modelName);
            body.put("messages", messages);
            body.put("temperature", 0.7);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            
            Map response = restTemplate.postForObject(baseUrl + "/chat/completions", request, Map.class);
            
            if (response != null && response.containsKey("choices")) {
                List choices = (List) response.get("choices");
                if (!choices.isEmpty()) {
                    Map choice = (Map) choices.get(0);
                    Map message = (Map) choice.get("message");
                    return (String) message.get("content");
                }
            }
        } catch (Exception e) {
            return "Error calling AI: " + e.getMessage();
        }
        return "Sorry, I couldn't process your request right now.";
    }
}

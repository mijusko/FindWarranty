package com.findwarranty.backend.controller;

import com.findwarranty.backend.model.ChatMessage;
import com.findwarranty.backend.model.User;
import com.findwarranty.backend.repository.UserRepository;
import com.findwarranty.backend.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/history/{username}")
    public ResponseEntity<?> getHistory(@PathVariable String username) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        List<ChatMessage> history = chatService.getChatHistory(user.get());
        return ResponseEntity.ok(history);
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, Object> request) {
        String username = (String) request.get("username");
        String content = (String) request.get("content");
        boolean includeReceipts = (boolean) request.getOrDefault("includeReceipts", false);

        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        ChatMessage response = chatService.processUserMessage(user.get(), content, includeReceipts);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/clear/{username}")
    public ResponseEntity<?> clearChat(@PathVariable String username) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        chatService.clearHistory(user.get());
        return ResponseEntity.ok().build();
    }
}

package com.findwarranty.backend.repository;

import com.findwarranty.backend.model.ChatMessage;
import com.findwarranty.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByUserOrderByTimestampAsc(User user);
    void deleteByUser(User user);
}

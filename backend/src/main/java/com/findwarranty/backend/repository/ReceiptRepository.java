package com.findwarranty.backend.repository;

import com.findwarranty.backend.model.Receipt;
import com.findwarranty.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    List<Receipt> findByUserId(Long userId);
    List<Receipt> findByUserOrderByPurchaseDateDesc(User user);
}

package com.skillset.domain.port;

import com.skillset.domain.entity.Message;
import java.util.List;
import java.util.Optional;

public interface MessageRepositoryPort {
    Message save(Message message);
    Optional<Message> findById(String id);
    List<Message> findBySenderIdAndRecipientId(String senderId, String recipientId);
    List<Message> findByRecipientIdAndIsReadFalse(String recipientId);
    List<Message> findBySender_Id(String senderId);
    List<Message> findByRecipient_Id(String recipientId);
}

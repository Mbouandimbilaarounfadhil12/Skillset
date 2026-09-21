package com.skillset.infrastructure.persistence;

import com.skillset.domain.entity.Message;
import com.skillset.domain.port.MessageRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, String>, MessageRepositoryPort {
    List<Message> findBySenderIdAndRecipientId(String senderId, String recipientId);
    List<Message> findByRecipientIdAndIsReadFalse(String recipientId);
    List<Message> findBySender_Id(String senderId);
    List<Message> findByRecipient_Id(String recipientId);

    @Modifying
    @Query("DELETE FROM Message m WHERE m.sender.id = :userId OR m.recipient.id = :userId")
    void deleteBySenderIdOrRecipientId(@Param("userId") String userId);
}

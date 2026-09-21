package com.skillset.infrastructure.persistence;

import com.skillset.domain.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByUser_Id(String userId);
    void deleteByUser_Id(String userId);
}

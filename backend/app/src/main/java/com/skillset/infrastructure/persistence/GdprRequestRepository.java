package com.skillset.infrastructure.persistence;

import com.skillset.domain.entity.GdprRequest;
import com.skillset.domain.entity.GdprRequest.RequestType;
import com.skillset.domain.entity.GdprRequest.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GdprRequestRepository extends JpaRepository<GdprRequest, String> {
    List<GdprRequest> findByUserIdAndRequestType(String userId, RequestType requestType);
    void deleteByUserId(String userId);
    List<GdprRequest> findByStatus(RequestStatus status);
}

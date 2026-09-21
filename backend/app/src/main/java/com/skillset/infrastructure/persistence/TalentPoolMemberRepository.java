package com.skillset.infrastructure.persistence;

import com.skillset.domain.entity.TalentPoolMember;
import com.skillset.domain.entity.TalentPoolMemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TalentPoolMemberRepository extends JpaRepository<TalentPoolMember, String> {
    List<TalentPoolMember> findByPoolId(String poolId);

    List<TalentPoolMember> findByPoolIdAndStatus(String poolId, TalentPoolMemberStatus status);

    boolean existsByPoolIdAndCandidateId(String poolId, String candidateId);

    Page<TalentPoolMember> findByPoolIdOrderByAddedAtDesc(String poolId, Pageable pageable);
}

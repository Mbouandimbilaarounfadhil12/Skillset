package com.skillset.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "talent_pool_member")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TalentPoolMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String poolId;

    @Column(nullable = false)
    private String candidateId;

    @Column(nullable = false)
    private String addedBy;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TalentPoolMemberSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TalentPoolMemberStatus status;
}

package com.skillset.infrastructure.persistence;

import com.skillset.domain.entity.JobBoardPublication;
import com.skillset.domain.value.JobBoardPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobBoardPublicationRepository extends JpaRepository<JobBoardPublication, String> {

    /**
     * Trouve toutes les publications pour une offre donnée.
     *
     * @param jobListingId ID de l'offre
     * @return liste des publications
     */
    List<JobBoardPublication> findByJobListingId(String jobListingId);

    /**
     * Trouve la publication d'une offre sur un partenaire spécifique.
     *
     * @param jobListingId ID de l'offre
     * @param partner partenaire job board
     * @return publication si elle existe
     */
    Optional<JobBoardPublication> findByJobListingIdAndPartner(String jobListingId, JobBoardPartner partner);

    /**
     * Trouve toutes les publications réussies pour une offre.
     *
     * @param jobListingId ID de l'offre
     * @return liste des publications avec status PUBLISHED
     */
    @Query("SELECT p FROM JobBoardPublication p WHERE p.jobListingId = :jobListingId AND p.status = 'PUBLISHED'")
    List<JobBoardPublication> findPublishedByJobListingId(@Param("jobListingId") String jobListingId);

    /**
     * Marque une publication comme dépubliée (status = UNPUBLISHED).
     *
     * @param jobListingId ID de l'offre
     * @param partner partenaire
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE JobBoardPublication p
        SET p.status = 'UNPUBLISHED', p.updatedAt = CURRENT_TIMESTAMP 
        WHERE p.jobListingId = :jobListingId AND p.partner = :partner
        """)
    void markAsUnpublished(@Param("jobListingId") String jobListingId, @Param("partner") JobBoardPartner partner);

    /**
     * Marque toutes les publications d'une offre comme dépubliées.
     *
     * @param jobListingId ID de l'offre
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE JobBoardPublication p
        SET p.status = 'UNPUBLISHED', p.updatedAt = CURRENT_TIMESTAMP
        WHERE p.jobListingId = :jobListingId
        """)
    void markAllAsUnpublished(@Param("jobListingId") String jobListingId);

    /**
     * Compte les publications réussies d'une offre.
     *
     * @param jobListingId ID de l'offre
     * @return nombre de publications réussies
     */
    @Query("SELECT COUNT(p) FROM JobBoardPublication p WHERE p.jobListingId = :jobListingId AND p.status = 'PUBLISHED'")
    long countPublishedByJobListingId(@Param("jobListingId") String jobListingId);
}

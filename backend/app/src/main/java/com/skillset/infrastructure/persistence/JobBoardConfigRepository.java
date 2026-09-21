package com.skillset.infrastructure.persistence;

import com.skillset.domain.entity.JobBoardConfig;
import com.skillset.domain.value.JobBoardPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobBoardConfigRepository extends JpaRepository<JobBoardConfig, String> {

    /**
     * Trouve tous les job boards configurés pour un pays donné, triés par priorité.
     *
     * @param countryCode code ISO du pays (ex: FR, CM, SN)
     * @return liste des configurations actives, triée par priorité ascendante
     */
    @Query("SELECT c FROM JobBoardConfig c WHERE c.countryCode = :countryCode AND c.isActive = true ORDER BY c.priority ASC")
    List<JobBoardConfig> findActiveByCountry(@Param("countryCode") String countryCode);

    /**
     * Trouve la configuration pour un pays ET un partenaire spécifique.
     *
     * @param countryCode code ISO du pays
     * @param partner partenaire job board
     * @return configuration si elle existe et est active
     */
    Optional<JobBoardConfig> findByCountryCodeAndPartner(String countryCode, JobBoardPartner partner);

    /**
     * Trouve tous les job boards pour un pays ET un fallback universel (*),
     * triés par priorité.
     * Utilisé quand aucun mapping exact n'existe pour un pays.
     *
     * @param countryCode code ISO du pays
     * @return liste des configurations actives du pays + fallback universel
     */
    @Query("""
        SELECT c FROM JobBoardConfig c
        WHERE (c.countryCode = :countryCode OR c.countryCode = '*')
        AND c.isActive = true
        ORDER BY c.priority ASC
        """)
    List<JobBoardConfig> findActiveByCountryWithFallback(@Param("countryCode") String countryCode);

    /**
     * Trouve les configurations du fallback universel (*)
     *
     * @return liste des configurations universelles actives
     */
    @Query("SELECT c FROM JobBoardConfig c WHERE c.countryCode = '*' AND c.isActive = true ORDER BY c.priority ASC")
    List<JobBoardConfig> findUniversalFallback();

    /**
     * Désactive toutes les configurations pour un pays donné.
     *
     * @param countryCode code ISO du pays
     */
    //void deactivateByCountryCode(String countryCode);
}

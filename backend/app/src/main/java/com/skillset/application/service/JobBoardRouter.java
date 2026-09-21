package com.skillset.application.service;

import com.skillset.domain.entity.JobBoardConfig;
import com.skillset.domain.value.JobBoardPartner;
import com.skillset.infrastructure.persistence.JobBoardConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service de routage qui détermine les job boards à appeler pour une ou plusieurs cibles pays.
 * Consulte la table JobBoardConfig pour récupérer les partenaires avec leur priorité.
 *
 * Logique :
 * 1. Pour chaque pays cible, chercher les configs actives
 * 2. Si aucune config trouvée pour ce pays, appliquer le fallback universel (*)
 * 3. Dédupliquer et trier par priorité
 * 4. Retourner la liste ordonnée des partenaires
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobBoardRouter {

    private final JobBoardConfigRepository jobBoardConfigRepository;

    /**
     * Détermine les job boards à appeler pour une liste de pays cibles.
     *
     * @param targetCountries liste des codes ISO (ex: ["CM", "FR"])
     * @return liste dédupliquée et triée de partenaires à appeler
     */
    public List<JobBoardPartner> determineTargetPartners(List<String> targetCountries) {
        if (targetCountries == null || targetCountries.isEmpty()) {
            log.warn("No target countries provided, using universal fallback");
            return getUniversalFallback();
        }

        // Collecte tous les configs pour tous les pays cibles
        Set<JobBoardPartner> allPartners = new HashSet<>();
        Map<JobBoardPartner, Integer> minPriority = new HashMap<>();

        for (String countryCode : targetCountries) {
            List<JobBoardConfig> configs = jobBoardConfigRepository.findActiveByCountryWithFallback(countryCode);

            if (configs.isEmpty()) {
                log.warn("No job board configuration found for country {}, using fallback", countryCode);
                List<JobBoardConfig> fallbackConfigs = jobBoardConfigRepository.findUniversalFallback();
                configs = fallbackConfigs;
            }

            for (JobBoardConfig config : configs) {
                allPartners.add(config.getPartner());
                // Garder la plus petite priorité pour chaque partenaire
                minPriority.merge(config.getPartner(), config.getPriority(), Math::min);
            }
        }

        // Convertir en liste triée par priorité
        List<JobBoardPartner> result = allPartners.stream()
            .sorted(Comparator.comparingInt(minPriority::get))
            .collect(Collectors.toList());

        log.info("Routing {} countries to {} job board partners: {}",
            targetCountries.size(), result.size(), result);

        return result;
    }

    /**
     * Détermine les job boards pour un seul pays.
     *
     * @param countryCode code ISO (ex: CM)
     * @return liste triée de partenaires pour ce pays
     */
    public List<JobBoardPartner> determineTargetPartnersForCountry(String countryCode) {
        return determineTargetPartners(Collections.singletonList(countryCode));
    }

    /**
     * Retourne les configurations détaillées (avec métadonnées, priorité, etc.)
     * pour les partenaires à appeler.
     *
     * @param targetCountries liste des codes ISO
     * @return configurations détaillées triées par priorité
     */
    public List<JobBoardConfig> getDetailedConfigurations(List<String> targetCountries) {
        if (targetCountries == null || targetCountries.isEmpty()) {
            return jobBoardConfigRepository.findUniversalFallback();
        }

        Set<JobBoardConfig> allConfigs = new LinkedHashSet<>();

        for (String countryCode : targetCountries) {
            List<JobBoardConfig> configs = jobBoardConfigRepository.findActiveByCountryWithFallback(countryCode);
            allConfigs.addAll(configs);
        }

        return allConfigs.stream()
            .sorted(Comparator.comparingInt(JobBoardConfig::getPriority))
            .collect(Collectors.toList());
    }

    /**
     * Retourne le fallback universel (pays "*").
     *
     * @return liste de partenaires universels
     */
    private List<JobBoardPartner> getUniversalFallback() {
        return jobBoardConfigRepository.findUniversalFallback()
            .stream()
            .map(JobBoardConfig::getPartner)
            .collect(Collectors.toList());
    }

    /**
     * Vérifie si un partenaire est configuré pour un pays.
     *
     * @param countryCode code ISO
     * @param partner partenaire à vérifier
     * @return true si configuré et actif
     */
    public boolean isPartnerConfiguredForCountry(String countryCode, JobBoardPartner partner) {
        return jobBoardConfigRepository.findByCountryCodeAndPartner(countryCode, partner)
            .map(JobBoardConfig::getIsActive)
            .orElse(false);
    }
}

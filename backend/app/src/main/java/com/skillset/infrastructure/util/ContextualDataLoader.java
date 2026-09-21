package com.skillset.infrastructure.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * Charge et cache les données contextuelles des pays (devises, villes, salaires, etc.)
 * depuis contextual-data.json au démarrage.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContextualDataLoader {

    private final ObjectMapper objectMapper;
    private Map<String, Map<String, Object>> countriesCache = new HashMap<>();
    private Map<String, List<String>> citiesByCountryCache = new HashMap<>();
    private Map<String, List<String>> contractTypesByCountryCache = new HashMap<>();
    private Map<String, List<String>> workModesByCountryCache = new HashMap<>();
    private Map<String, Object> cvFormatCache = new HashMap<>();

    /**
     * Initialise le cache au startup
     */
    public void initialize() {
        try {
            ClassPathResource resource = new ClassPathResource("contextual-data.json");
            JsonNode root = objectMapper.readTree(resource.getInputStream());

            // Charger pays
            JsonNode countriesNode = root.get("countries");
            if (countriesNode != null && countriesNode.isArray()) {
                countriesNode.forEach(node -> {
                    String name = node.get("name").asText();
                    Map<String, Object> data = new HashMap<>();
                    data.put("code", node.get("code").asText());
                    data.put("currency", node.get("currency").asText());
                    data.put("currencySymbol", node.get("currencySymbol").asText());
                    data.put("phonePrefix", node.get("phonePrefix").asText());
                    data.put("salaryPeriod", node.get("salaryPeriod").asText());

                    List<String> languages = new ArrayList<>();
                    JsonNode langNode = node.get("languages");
                    if (langNode != null && langNode.isArray()) {
                        langNode.forEach(l -> languages.add(l.asText()));
                    }
                    data.put("languages", languages);

                    countriesCache.put(name, data);
                });
            }

            // Charger villes par pays
            JsonNode citiesNode = root.get("citiesByCountry");
            if (citiesNode != null) {
                citiesNode.fields().forEachRemaining(entry -> {
                    List<String> cities = new ArrayList<>();
                    entry.getValue().forEach(node -> cities.add(node.asText()));
                    citiesByCountryCache.put(entry.getKey(), cities);
                });
            }

            // Charger types contrats par pays
            JsonNode contractsNode = root.get("contractTypesByCountry");
            if (contractsNode != null) {
                contractsNode.fields().forEachRemaining(entry -> {
                    List<String> contracts = new ArrayList<>();
                    entry.getValue().forEach(node -> contracts.add(node.asText()));
                    contractTypesByCountryCache.put(entry.getKey(), contracts);
                });
            }

            // Charger modes travail par pays
            JsonNode workModesNode = root.get("workModesByCountry");
            if (workModesNode != null) {
                workModesNode.fields().forEachRemaining(entry -> {
                    List<String> modes = new ArrayList<>();
                    entry.getValue().forEach(node -> modes.add(node.asText()));
                    workModesByCountryCache.put(entry.getKey(), modes);
                });
            }

            // Charger formats CV par pays
            JsonNode cvFormatNode = root.get("cvFormatByCountry");
            if (cvFormatNode != null) {
                cvFormatCache.put("withPhoto", convertToList(cvFormatNode.get("withPhoto")));
                cvFormatCache.put("withPersonalDetails", convertToList(cvFormatNode.get("withPersonalDetails")));
                cvFormatCache.put("rgpdStrict", convertToList(cvFormatNode.get("rgpdStrict")));
            }

            log.info("ContextualDataLoader initialized: {} pays chargés", countriesCache.size());
        } catch (IOException e) {
            log.error("Erreur chargement contextual-data.json", e);
        }
    }

    /**
     * Retourne les métadonnées d'un pays
     * Format retourné:
     * {
     *   "code": "FR",
     *   "currency": "EUR",
     *   "currencySymbol": "€",
     *   "phonePrefix": "+33",
     *   "salaryPeriod": "annuel",
     *   "languages": ["Français"],
     *   "cities": ["Paris", "Lyon", ...],
     *   "salaryRanges": ["18 000 - 25 000 €/an", ...],
     *   "contractTypes": ["CDI", "CDD", ...],
     *   "workModes": ["Présentiel", "Hybride", ...],
     *   "withPhoto": ["CM", "SN", ...],
     *   "withPersonalDetails": ["CM", ...],
     *   "rgpdStrict": ["FR", "BE", ...]
     * }
     */
    public Map<String, Object> getCountryData(String countryName) {
        if (!countriesCache.containsKey(countryName)) {
            return null;
        }

        Map<String, Object> data = new HashMap<>(countriesCache.get(countryName));

        // Ajouter villes disponibles
        String code = (String) data.get("code");
        data.put("cities", citiesByCountryCache.getOrDefault(code, Collections.emptyList()));

        // Ajouter fourchettes salariales (chargées séparément si nécessaire)
        data.put("salaryRanges", getSalaryRanges(code));

        // Ajouter types contrats
        data.put("contractTypes", contractTypesByCountryCache.getOrDefault(code, Collections.emptyList()));

        // Ajouter modes travail
        data.put("workModes", workModesByCountryCache.getOrDefault(code, Collections.emptyList()));

        // Ajouter formats CV
        data.putAll(cvFormatCache);

        return data;
    }

    /**
     * Retourne les fourchettes salariales pour un code pays
     */
    public List<String> getSalaryRanges(String countryCode) {
        // Hardcodé pour l'instant, peut être chargé depuis JSON si nécessaire
        Map<String, List<String>> ranges = Map.ofEntries(
                Map.entry("CM", List.of("50 000 - 150 000 FCFA/mois", "150 000 - 300 000 FCFA/mois", "300 000 - 500 000 FCFA/mois", "500 000 - 1 000 000 FCFA/mois", "+ 1 000 000 FCFA/mois")),
                Map.entry("FR", List.of("18 000 - 25 000 €/an", "25 000 - 35 000 €/an", "35 000 - 50 000 €/an", "50 000 - 70 000 €/an", "+ 70 000 €/an")),
                Map.entry("SN", List.of("50 000 - 150 000 FCFA/mois", "150 000 - 300 000 FCFA/mois", "300 000 - 600 000 FCFA/mois", "600 000 - 1 000 000 FCFA/mois", "+ 1 000 000 FCFA/mois")),
                Map.entry("CI", List.of("50 000 - 150 000 FCFA/mois", "150 000 - 300 000 FCFA/mois", "300 000 - 600 000 FCFA/mois", "600 000 - 1 000 000 FCFA/mois", "+ 1 000 000 FCFA/mois")),
                Map.entry("MA", List.of("3 000 - 6 000 DH/mois", "6 000 - 10 000 DH/mois", "10 000 - 20 000 DH/mois", "20 000 - 40 000 DH/mois", "+ 40 000 DH/mois")),
                Map.entry("TN", List.of("1 000 - 2 000 DT/mois", "2 000 - 4 000 DT/mois", "4 000 - 8 000 DT/mois", "8 000 - 15 000 DT/mois", "+ 15 000 DT/mois")),
                Map.entry("BE", List.of("20 000 - 30 000 €/an", "30 000 - 45 000 €/an", "45 000 - 60 000 €/an", "60 000 - 80 000 €/an", "+ 80 000 €/an")),
                Map.entry("CH", List.of("60 000 - 80 000 CHF/an", "80 000 - 100 000 CHF/an", "100 000 - 130 000 CHF/an", "130 000 - 170 000 CHF/an", "+ 170 000 CHF/an")),
                Map.entry("GA", List.of("50 000 - 150 000 FCFA/mois", "150 000 - 300 000 FCFA/mois", "300 000 - 500 000 FCFA/mois", "500 000 - 1 000 000 FCFA/mois", "+ 1 000 000 FCFA/mois")),
                Map.entry("CD", List.of("100 000 - 300 000 FC/mois", "300 000 - 600 000 FC/mois", "600 000 - 1 000 000 FC/mois", "1 000 000 - 2 000 000 FC/mois", "+ 2 000 000 FC/mois")),
                Map.entry("CG", List.of("50 000 - 150 000 FCFA/mois", "150 000 - 300 000 FCFA/mois", "300 000 - 500 000 FCFA/mois", "500 000 - 1 000 000 FCFA/mois", "+ 1 000 000 FCFA/mois")),
                Map.entry("CA", List.of("35 000 - 50 000 CA$/an", "50 000 - 70 000 CA$/an", "70 000 - 100 000 CA$/an", "100 000 - 140 000 CA$/an", "+ 140 000 CA$/an"))
        );
        return ranges.getOrDefault(countryCode, Collections.emptyList());
    }

    /**
     * Retourne tous les pays disponibles
     */
    public Collection<String> getAllCountries() {
        return countriesCache.keySet();
    }

    // ─── PRIVATE HELPERS ───

    private List<String> convertToList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(n -> list.add(n.asText()));
        }
        return list;
    }
}

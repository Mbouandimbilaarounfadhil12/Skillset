package com.skillset.infrastructure.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CVParserUtil")
class CVParserUtilTest {

    private CVParserUtil util;

    @BeforeEach
    void setUp() {
        util = new CVParserUtil();
    }

    // ── extractKeywords ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("extractKeywords()")
    class ExtractKeywords {

        @Test
        @DisplayName("retourne une chaîne vide pour une entrée null")
        void nullInput_returnsEmpty() {
            assertThat(util.extractKeywords(null)).isEmpty();
        }

        @Test
        @DisplayName("retourne une chaîne vide pour une entrée vide")
        void blankInput_returnsEmpty() {
            assertThat(util.extractKeywords("   ")).isEmpty();
        }

        @Test
        @DisplayName("filtre les stop-words français et anglais")
        void filtersStopWords() {
            String result = util.extractKeywords("le développeur est très compétent");
            assertThat(result).doesNotContain("le", "est");
            assertThat(result).contains("développeur", "très", "compétent");
        }

        @Test
        @DisplayName("filtre les tokens de moins de 3 caractères")
        void filtersShortTokens() {
            String result = util.extractKeywords("Java is a programming language");
            List<String> tokens = java.util.Arrays.asList(result.split(","));
            assertThat(tokens).doesNotContain("is", "a");
        }

        @Test
        @DisplayName("retourne des tokens distincts (pas de doublons)")
        void returnsDistinctTokens() {
            String result = util.extractKeywords("java java java spring spring");
            long javaCount = Arrays.stream(result.split(","))
                    .filter("java"::equals).count();
            assertThat(javaCount).isEqualTo(1);
        }

        @Test
        @DisplayName("extrait les mots-clés d'un texte technique réaliste")
        void extractsTechnicalKeywords() {
            String cv = "Développeur Java Spring Boot PostgreSQL microservices Docker";
            String result = util.extractKeywords(cv);
            assertThat(result).contains("java", "spring", "boot", "postgresql", "microservices", "docker");
        }
    }

    // ── calculateMatchScore ────────────────────────────────────────────────────

    @Nested
    @DisplayName("calculateMatchScore()")
    class CalculateMatchScore {

        @Test
        @DisplayName("retourne 0.0 si le CV est null")
        void nullCv_returnsZero() {
            assertThat(util.calculateMatchScore(null, "Java Spring")).isEqualTo(0.0);
        }

        @Test
        @DisplayName("retourne 0.0 si les prérequis du poste sont null")
        void nullJob_returnsZero() {
            assertThat(util.calculateMatchScore("Java Spring", null)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("retourne 0.0 si les deux entrées sont vides")
        void blankInputs_returnsZero() {
            assertThat(util.calculateMatchScore("", "")).isEqualTo(0.0);
        }

        @Test
        @DisplayName("retourne 0.0 si les prérequis du poste sont vides")
        void emptyJobRequirements_returnsZero() {
            assertThat(util.calculateMatchScore("Java Spring Boot", "")).isEqualTo(0.0);
        }

        @Test
        @DisplayName("retourne 100.0 quand le CV couvre tous les mots-clés du poste")
        void perfectMatch_returns100() {
            String jobReqs = "Java Spring PostgreSQL Docker";
            String cv      = "Java Spring PostgreSQL Docker microservices";
            assertThat(util.calculateMatchScore(cv, jobReqs)).isEqualTo(100.0);
        }

        @Test
        @DisplayName("retourne 0.0 quand aucun mot-clé ne correspond")
        void noMatch_returnsZero() {
            assertThat(util.calculateMatchScore("Python Django", "Java Spring")).isEqualTo(0.0);
        }

        @Test
        @DisplayName("retourne un score partiel proportionnel aux correspondances")
        void partialMatch_returnsProportionalScore() {
            // 2 mots-clés requis : Java, Spring → CV couvre Java seulement → 50 %
            double score = util.calculateMatchScore("java developer senior", "java spring");
            assertThat(score).isEqualTo(50.0);
        }

        @Test
        @DisplayName("arrondit le score à 2 décimales")
        void scoreIsRoundedToTwoDecimals() {
            // 3 mots requis, 1 dans le CV → 1/3 * 100 = 33.33…% → arrondi à 33.33
            double score = util.calculateMatchScore("java developer", "java spring docker");
            assertThat(score).isEqualTo(33.33);
        }

        @Test
        @DisplayName("la comparaison est insensible à la casse")
        void matchIsCaseInsensitive() {
            double score = util.calculateMatchScore("JAVA SPRING BOOT", "java spring boot");
            assertThat(score).isEqualTo(100.0);
        }
    }
}

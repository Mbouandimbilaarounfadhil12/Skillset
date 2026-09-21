package com.skillset.application.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie que la génération PDF via openhtmltopdf produit un vrai fichier PDF
 * (et pas le placeholder texte historique), y compris avec des caractères accentués français.
 */
class CvGeneratorServicePdfTest {

    @Test
    void generatePdfFromHtml_producesValidPdfBytes() throws Exception {
        CvGeneratorService service = new CvGeneratorService(null, null, null, null, null);
        Method method = CvGeneratorService.class.getDeclaredMethod("generatePdfFromHtml", String.class);
        method.setAccessible(true);

        String html = "<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8' />"
                + "<style>body { font-family: Arial, Helvetica, sans-serif; }</style></head>"
                + "<body>"
                + "<h1>Amélie Dupont-Béranger</h1>"
                + "<p>Développeuse Java Spring — 5 ans d'expérience</p>"
                + "<p>Email : amelie@example.com — Tél. : +33 6 12 34 56 78 — Lieu : Paris, France</p>"
                + "<p>Compétences : Java, Spring Boot, Kubernetes, gestion d'équipe</p>"
                + "</body></html>";

        byte[] pdfBytes = (byte[]) method.invoke(service, html);

        assertThat(pdfBytes).isNotEmpty();
        assertThat(pdfBytes.length).isGreaterThan(500); // un vrai PDF, pas juste "PDF_PLACEHOLDER"

        String header = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF-");
    }

    @Test
    void buildCvHtmlThenGeneratePdf_endToEndProducesValidPdf() throws Exception {
        CvGeneratorService service = new CvGeneratorService(null, null, null, null, null);

        Map<String, Object> cvData = Map.of(
                "withPhoto", false,
                "personalInfo", Map.of(
                        "nom", "Dupont-Béranger",
                        "prenom", "Amélie",
                        "titre", "Développeuse Java Senior",
                        "email", "amelie@example.com",
                        "telephone", "+33 6 12 34 56 78",
                        "localisation", "Paris, France"
                ),
                "professionalSummary", "5 ans d'expérience en développement Java/Spring, spécialisée microservices.",
                "experience", List.of(Map.of(
                        "poste", "Développeuse Backend",
                        "entreprise", "Acme Corp",
                        "lieu", "Paris",
                        "periode", "2021-2026"
                )),
                "skills", Map.of("technical", List.of("Java", "Spring Boot", "Kubernetes"))
        );

        Method buildHtml = CvGeneratorService.class.getDeclaredMethod("buildCvHtml", Map.class);
        buildHtml.setAccessible(true);
        String html = (String) buildHtml.invoke(service, cvData);

        Method generatePdf = CvGeneratorService.class.getDeclaredMethod("generatePdfFromHtml", String.class);
        generatePdf.setAccessible(true);
        byte[] pdfBytes = (byte[]) generatePdf.invoke(service, html);

        assertThat(pdfBytes).isNotEmpty();
        String header = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF-");
    }
}

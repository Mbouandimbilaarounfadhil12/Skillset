package com.skillset.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.skillset.domain.entity.CandidateProfile;
import com.skillset.domain.entity.User;
import com.skillset.domain.port.CandidateProfileRepositoryPort;
import com.skillset.domain.port.UserRepositoryPort;
import com.skillset.infrastructure.util.CloudinaryService;
import com.skillset.infrastructure.util.EmailUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service pour générer des CVs professionnels au format PDF.
 * - Génère template HTML adapté au pays (avec/sans photo, RGPD)
 * - Convertit HTML → PDF via openhtmltopdf
 * - Upload PDF sur Cloudinary
 * - Envoie email au candidat avec lien + conseils ATS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CvGeneratorService {

    private final CloudinaryService cloudinaryService;
    private final EmailUtil emailUtil;
    private final CandidateProfileRepositoryPort candidateProfileRepository;
    private final UserRepositoryPort userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Orchestré l'ensemble du workflow : génération → PDF → upload → email
     */
    public String generateAndDeliverCv(String userId, Map<String, Object> cvData) {
        try {
            // 1. Extraire données pour template
            String html = buildCvHtml(cvData);
            log.debug("CV HTML généré pour user {}", userId);

            // 2. Générer PDF depuis HTML
            byte[] pdfBytes = generatePdfFromHtml(html);
            log.info("CV PDF généré ({} bytes)", pdfBytes.length);

            // 3. Upload sur Cloudinary (si configuré)
            String cvUrl = uploadCvToCloudinary(userId, pdfBytes, cvData);
            if (cvUrl != null) {
                log.info("CV uploadé: {}", cvUrl);
                // 4. Mettre à jour profil candidat + 5. Envoyer email avec CV (uniquement possible avec une vraie URL)
                updateCandidateProfile(userId, cvUrl);
                sendCvEmail(userId, cvUrl, cvData);
                log.info("CV généré et livré avec succès pour user {}", userId);
                return cvUrl;
            }

            // Cloudinary non configuré (ex. environnement de dev) : on renvoie quand même
            // le PDF généré directement au navigateur via une data URI, plutôt que d'échouer.
            log.warn("Cloudinary non configuré — le CV est renvoyé directement en data URI pour user {} "
                    + "(non stocké, pas d'email envoyé)", userId);
            return "data:application/pdf;base64," + Base64.getEncoder().encodeToString(pdfBytes);
        } catch (Exception e) {
            log.error("Erreur génération CV pour user {}", userId, e);
            return null;
        }
    }

    // ─── PRIVATE HELPERS ───

    /**
     * Construit le template HTML du CV adapté au pays
     * Différenciation:
     * - Avec photo (pays africains)
     * - Sans photo + RGPD strict (France, Belgique, Suisse, Canada)
     * - Avec/sans informations personnelles (âge, nationalité)
     */
    private String buildCvHtml(Map<String, Object> cvData) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='fr'>\n");
        html.append("<head>\n");
        html.append("  <meta charset='UTF-8' />\n");
        html.append("  <title>CV - SkillSet</title>\n");
        html.append("  <style>\n");
        html.append("    @page { size: A4; margin: 0; }\n");
        html.append("    * { margin: 0; padding: 0; box-sizing: border-box; }\n");
        html.append("    body { font-family: Arial, Helvetica, sans-serif; font-size: 11px; line-height: 1.4; color: #333; }\n");
        html.append("    .container { max-width: 210mm; height: 297mm; margin: 0 auto; padding: 15mm; position: relative; }\n");
        html.append("    .header { display: table; width: 100%; margin-bottom: 8mm; border-bottom: 2px solid #2B5F8E; padding-bottom: 5mm; }\n");
        html.append("    .photo { display: table-cell; width: 30mm; height: 30mm; background: #f0f0f0; border-radius: 2px; vertical-align: top; }\n");
        html.append("    .header-text { display: table-cell; vertical-align: top; padding-left: 8mm; }\n");
        html.append("    .header-text h1 { font-size: 16px; color: #2B5F8E; margin-bottom: 2px; }\n");
        html.append("    .header-text .job-title { font-size: 12px; color: #666; margin-bottom: 4px; }\n");
        html.append("    .header-text .contact { font-size: 10px; color: #888; }\n");
        html.append("    .header-text .contact span { margin-right: 12px; }\n");
        html.append("    .section { margin-bottom: 6mm; }\n");
        html.append("    .section-title { font-size: 12px; font-weight: bold; color: #2B5F8E; border-bottom: 1px solid #ddd; margin-bottom: 2mm; padding-bottom: 1mm; }\n");
        html.append("    .entry { margin-bottom: 3mm; }\n");
        html.append("    .entry-title { font-weight: bold; font-size: 11px; }\n");
        html.append("    .entry-subtitle { font-size: 10px; color: #666; font-style: italic; }\n");
        html.append("    .entry-text { font-size: 10px; margin-top: 1mm; line-height: 1.3; }\n");
        html.append("    .skills { font-size: 10px; }\n");
        html.append("    .skill-tag { display: inline-block; background: #f0f0f0; padding: 1mm 2mm; border-radius: 1px; margin: 0 2mm 2mm 0; }\n");
        html.append("    .footer { font-size: 8px; color: #999; margin-top: 10mm; padding-top: 5mm; border-top: 1px solid #ddd; text-align: center; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("  <div class='container'>\n");

        // HEADER
        boolean withPhoto = (boolean) cvData.getOrDefault("withPhoto", false);
        Map<String, Object> personal = (Map<String, Object>) cvData.get("personalInfo");

        html.append("    <div class='header'>\n");
        if (withPhoto) {
            html.append("      <div class='photo'>[PHOTO]</div>\n");
        }
        html.append("      <div class='header-text'>\n");
        html.append("        <h1>").append(personal != null ? personal.get("nom") : "").append(" ")
                .append(personal != null ? personal.get("prenom") : "").append("</h1>\n");
        html.append("        <div class='job-title'>").append(personal != null ? personal.get("titre") : "").append("</div>\n");
        html.append("        <div class='contact'>\n");
        if (personal != null) {
            if (personal.get("email") != null) html.append("          <span>Email : ").append(personal.get("email")).append("</span>\n");
            if (personal.get("telephone") != null) html.append("          <span>Tél. : ").append(personal.get("telephone")).append("</span>\n");
            if (personal.get("localisation") != null) html.append("          <span>Lieu : ").append(personal.get("localisation")).append("</span>\n");
        }
        html.append("        </div>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");

        // PROFESSIONNEL SUMMARY
        String summary = (String) cvData.get("professionalSummary");
        if (summary != null && !summary.isEmpty()) {
            html.append("    <div class='section'>\n");
            html.append("      <div class='section-title'>PROFESSIONNEL</div>\n");
            html.append("      <div class='entry-text'>").append(summary).append("</div>\n");
            html.append("    </div>\n");
        }

        // EXPERIENCE
        List<Map<String, Object>> experiences = (List<Map<String, Object>>) cvData.get("experience");
        if (experiences != null && !experiences.isEmpty()) {
            html.append("    <div class='section'>\n");
            html.append("      <div class='section-title'>EXPÉRIENCE</div>\n");
            for (Map<String, Object> exp : experiences) {
                html.append("      <div class='entry'>\n");
                html.append("        <div class='entry-title'>").append(exp.get("poste")).append("</div>\n");
                html.append("        <div class='entry-subtitle'>").append(exp.get("entreprise")).append(" - ").append(exp.get("lieu")).append("</div>\n");
                html.append("        <div class='entry-subtitle'>").append(exp.get("periode")).append("</div>\n");
                html.append("      </div>\n");
            }
            html.append("    </div>\n");
        }

        // SKILLS
        Map<String, Object> skills = (Map<String, Object>) cvData.get("skills");
        if (skills != null) {
            html.append("    <div class='section'>\n");
            html.append("      <div class='section-title'>COMPÉTENCES</div>\n");
            html.append("      <div class='skills'>\n");
            List<String> technical = (List<String>) skills.get("technical");
            if (technical != null) {
                for (String skill : technical) {
                    html.append("        <span class='skill-tag'>").append(skill).append("</span>\n");
                }
            }
            html.append("      </div>\n");
            html.append("    </div>\n");
        }

        // FOOTER
        html.append("    <div class='footer'>\n");
        html.append("      <p>CV généré par SkillSet — ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</p>\n");
        html.append("    </div>\n");
        html.append("  </div>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    /**
     * Génère un PDF depuis HTML via openhtmltopdf.
     */
    private byte[] generatePdfFromHtml(String html) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(html, null);
        builder.toStream(outputStream);
        builder.run();
        return outputStream.toByteArray();
    }

    /**
     * Upload le PDF sur Cloudinary
     */
    private String uploadCvToCloudinary(String userId, byte[] pdfBytes, Map<String, Object> cvData) {
        String firstName = "";
        String lastName = "";
        Map<String, Object> personal = (Map<String, Object>) cvData.get("personalInfo");
        if (personal != null) {
            firstName = String.valueOf(personal.getOrDefault("prenom", ""));
            lastName = String.valueOf(personal.getOrDefault("nom", ""));
        }

        String publicId = String.format("skillset/cvs/%s/cv_%s%s_%d",
                userId,
                firstName.toLowerCase().replaceAll("[^a-z]", ""),
                lastName.toLowerCase().replaceAll("[^a-z]", ""),
                System.currentTimeMillis());

        return cloudinaryService.uploadPdfBytes(pdfBytes, publicId);
    }

    /**
     * Met à jour le profil candidat avec l'URL CV
     */
    private void updateCandidateProfile(String userId, String cvUrl) {
        Optional<CandidateProfile> profileOpt = candidateProfileRepository.findByUserId(userId);
        if (profileOpt.isPresent()) {
            CandidateProfile profile = profileOpt.get();
            profile.setCvUrl(cvUrl); // À ajouter à CandidateProfile
            candidateProfileRepository.save(profile);
            log.info("Profil candidat mis à jour avec CV: {}", cvUrl);
        }
    }

    /**
     * Envoie un email au candidat avec son CV
     */
    private void sendCvEmail(String userId, String cvUrl, Map<String, Object> cvData) {
        Optional<User> userOpt = userRepository.findUserById(userId);
        if (userOpt.isEmpty()) {
            log.warn("User {} introuvable pour envoi email CV", userId);
            return;
        }

        User user = userOpt.get();
        String subject = "Votre CV SkillSet est prêt !";
        String body = buildCvEmailBody(user, cvUrl, cvData);

        try {
            emailUtil.sendNotification(user.getEmail(), subject, body);
            log.info("Email CV envoyé à {}", user.getEmail());
        } catch (Exception e) {
            log.error("Erreur envoi email CV", e);
        }
    }

    /**
     * Construit le corps du mail avec conseils ATS
     */
    private String buildCvEmailBody(User user, String cvUrl, Map<String, Object> cvData) {
        String country = (String) cvData.get("country");
        StringBuilder body = new StringBuilder();

        body.append("Bonjour ").append(user.getFirstName()).append(",\n\n");
        body.append("Votre CV SkillSet a été généré avec succès !\n\n");

        body.append("📥 Télécharger votre CV:\n");
        body.append(cvUrl).append("\n\n");

        body.append("📋 Aperçu de vos informations:\n");
        Map<String, Object> personal = (Map<String, Object>) cvData.get("personalInfo");
        if (personal != null) {
            body.append("- Titre: ").append(personal.getOrDefault("titre", "N/A")).append("\n");
            body.append("- Localisation: ").append(personal.getOrDefault("localisation", "N/A")).append("\n");
        }

        body.append("\n🎯 Conseils ATS pour ").append(country != null ? country : "votre pays").append(":\n");
        body.append("- Vérifiez que les mots-clés du secteur sont présents\n");
        body.append("- Utilisez des listes à puces plutôt que des blocs de texte\n");
        body.append("- Mentionnez vos compétences techniques explicitement\n");
        body.append("- Incluez des chiffres et résultats mesurables\n\n");

        body.append("Vos offres personnalisées vous attendent sur SkillSet !\n\n");

        body.append("Cordialement,\n");
        body.append("L'équipe SkillSet");

        return body.toString();
    }
}

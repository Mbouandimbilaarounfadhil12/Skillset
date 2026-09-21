package com.skillset.infrastructure.util;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Component
public class EmailUtil {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*\\.[a-zA-Z]{2,}$");

    private final JavaMailSender mailSender;

    public EmailUtil(Optional<JavaMailSender> mailSender) {
        this.mailSender = mailSender.orElse(null);
    }

    public boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public void sendNotification(String to, String subject, String body) {
        if (mailSender == null) {
            log.warn("JavaMailSender not configured — skipping email to {}: {}", to, subject);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Email sent to: {} — {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    public void sendApplicationConfirmation(String candidateEmail, String jobTitle) {
        sendNotification(
            candidateEmail,
            "Candidature reçue — " + jobTitle,
            "Bonjour,\n\n" +
            "Votre candidature pour le poste de « " + jobTitle + " » a bien été reçue.\n" +
            "Nous vous recontacterons prochainement.\n\n" +
            "Cordialement,\nL'équipe SkillSet"
        );
    }

    public void sendInterviewInvitation(String candidateEmail, String companyName, String interviewDate) {
        sendNotification(
            candidateEmail,
            "Invitation à un entretien — " + companyName,
            "Bonjour,\n\n" +
            "Vous êtes invité(e) à un entretien avec " + companyName + " le " + interviewDate + ".\n" +
            "Veuillez vous connecter à votre espace SkillSet pour confirmer votre disponibilité.\n\n" +
            "Cordialement,\nL'équipe SkillSet"
        );
    }

    public void sendDataExportConfirmation(String to, String firstName) {
        try {
            if (mailSender == null) {
                log.warn("Email non configuré — sendDataExportConfirmation ignoré");
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("SkillSet — Export de vos données personnelles");
            message.setText(
                "Bonjour " + firstName + ",\n\n" +
                "Votre demande d'export de données personnelles (Article 20 RGPD) a bien été reçue.\n\n" +
                "Vos données sont en cours de préparation. Vous recevrez un email de confirmation une fois l'export finalisé.\n\n" +
                "La génération peut prendre quelques minutes.\n\n" +
                "Cordialement,\n" +
                "L'équipe SkillSet"
            );
            mailSender.send(message);
            log.info("Email export données envoyé à {}", to);
        } catch (Exception e) {
            log.error("Erreur envoi email export données à {} : {}", to, e.getMessage());
        }
    }

    public void sendAccountDeletionConfirmation(String to, String firstName) {
        try {
            if (mailSender == null) {
                log.warn("Email non configuré — sendAccountDeletionConfirmation ignoré");
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("SkillSet — Confirmation de suppression de compte");
            message.setText(
                "Bonjour " + firstName + ",\n\n" +
                "Nous confirmons que votre demande de suppression de compte a bien été prise en compte.\n\n" +
                "Toutes vos données personnelles seront définitivement supprimées conformément au RGPD (Article 17).\n\n" +
                "Nous sommes désolés de vous voir partir et espérons vous revoir à l'avenir.\n\n" +
                "Cordialement,\n" +
                "L'équipe SkillSet"
            );
            mailSender.send(message);
            log.info("Email suppression compte envoyé à {}", to);
        } catch (Exception e) {
            log.error("Erreur envoi email suppression compte à {} : {}", to, e.getMessage());
        }
    }

    public void sendStatusChangedEmail(String to, String firstName, String jobTitle, String newStatus) {
        if (mailSender == null) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Mise à jour de votre candidature — SkillSet");
        message.setText(
            "Bonjour " + firstName + ",\n\n" +
            "Votre candidature pour le poste de « " + jobTitle + " » a été mise à jour.\n" +
            "Nouveau statut : " + newStatus + "\n\n" +
            "Connectez-vous à votre espace SkillSet pour en savoir plus.\n\n" +
            "Cordialement,\n" +
            "L'équipe SkillSet"
        );
        mailSender.send(message);
    }

    public void sendBulkActionConfirmation(String to, String firstName, int count) {
        if (mailSender == null) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Mise à jour en masse effectuée — SkillSet");
        message.setText(
            "Bonjour " + firstName + ",\n\n" +
            "Vous venez de mettre à jour le statut de " + count + " candidature(s) depuis votre tableau de bord SkillSet.\n\n" +
            "Cordialement,\n" +
            "L'équipe SkillSet"
        );
        mailSender.send(message);
    }

    public void sendAutomationTriggeredEmail(String to, String firstName, String ruleName, String jobTitle) {
        if (mailSender == null) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Action automatique sur votre candidature — SkillSet");
        message.setText(
            "Bonjour " + firstName + ",\n\n" +
            "Une action automatique « " + ruleName + " » vient d'être appliquée à votre candidature pour le poste de « " + jobTitle + " ».\n\n" +
            "Connectez-vous à votre espace SkillSet pour consulter les détails.\n\n" +
            "Cordialement,\n" +
            "L'équipe SkillSet"
        );
        mailSender.send(message);
    }

    public void sendLoginNotification(String to, String firstName) {
        if (mailSender == null || !isValidEmail(to)) {
            log.warn("Email non configuré ou adresse invalide — sendLoginNotification ignoré pour {}", to);
            return;
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Bienvenue sur SkillSet !");
            helper.setText(buildLoginEmailHtml(firstName), true);
            mailSender.send(mimeMessage);
            log.info("Email de bienvenue/connexion envoyé à {}", to);
        } catch (Exception e) {
            log.error("Erreur envoi email de connexion à {} : {}", to, e.getMessage());
        }
    }

    private String buildLoginEmailHtml(String firstName) {
        return "<div style=\"font-family:Arial,Helvetica,sans-serif;max-width:480px;margin:0 auto;padding:32px 24px;background:#ffffff;\">"
             + "<table cellpadding=\"0\" cellspacing=\"0\" role=\"presentation\" style=\"border-collapse:collapse;margin-bottom:28px;\"><tr>"
             + "<td style=\"width:36px;height:36px;background-color:#C42033;color:#ffffff;font-weight:700;font-size:18px;"
             + "border-radius:8px;text-align:center;vertical-align:middle;font-family:Arial,Helvetica,sans-serif;\">S</td>"
             + "<td style=\"padding-left:10px;font-size:22px;font-weight:700;color:#1a1a1a;font-family:Arial,Helvetica,sans-serif;\">SkillSet</td>"
             + "</tr></table>"
             + "<h2 style=\"color:#1a1a1a;margin:0 0 12px;\">Bienvenue sur SkillSet, " + firstName + " !</h2>"
             + "<p style=\"color:#444444;line-height:1.6;\">SkillSet est la plateforme de recrutement qu'il vous faut : "
             + "elle connecte candidats et entreprises grâce à un système de matching intelligent.</p>"
             + "<p style=\"color:#444444;line-height:1.6;\">Merci de votre confiance et bienvenue parmi nous. "
             + "Nous vous confirmons qu'une connexion vient d'avoir lieu sur votre compte.</p>"
             + "<p style=\"color:#888888;font-size:13px;line-height:1.6;margin-top:24px;\">Si vous n'êtes pas à l'origine de "
             + "cette connexion, changez immédiatement votre mot de passe et activez la double authentification depuis vos réglages.</p>"
             + "<p style=\"color:#444444;margin-top:24px;\">Cordialement,<br/>L'équipe SkillSet</p>"
             + "</div>";
    }

    public void sendOtpCode(String to, String firstName, String code) {
        if (mailSender == null || !isValidEmail(to)) {
            log.warn("Email non configuré ou adresse invalide — code de vérification non envoyé pour {}", to);
            return;
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Votre code de vérification SkillSet");
            helper.setText(buildOtpEmailHtml(firstName, code), true);
            mailSender.send(mimeMessage);
            log.info("Code de vérification envoyé à {}", to);
        } catch (Exception e) {
            log.error("Erreur envoi code de vérification à {} : {}", to, e.getMessage());
        }
    }

    private String buildOtpEmailHtml(String firstName, String code) {
        return "<div style=\"font-family:Arial,Helvetica,sans-serif;max-width:480px;margin:0 auto;padding:32px 24px;background:#ffffff;\">"
             + "<table cellpadding=\"0\" cellspacing=\"0\" role=\"presentation\" style=\"border-collapse:collapse;margin-bottom:28px;\"><tr>"
             + "<td style=\"width:36px;height:36px;background-color:#C42033;color:#ffffff;font-weight:700;font-size:18px;"
             + "border-radius:8px;text-align:center;vertical-align:middle;font-family:Arial,Helvetica,sans-serif;\">S</td>"
             + "<td style=\"padding-left:10px;font-size:22px;font-weight:700;color:#1a1a1a;font-family:Arial,Helvetica,sans-serif;\">SkillSet</td>"
             + "</tr></table>"
             + "<h2 style=\"color:#1a1a1a;margin:0 0 12px;\">Bonjour " + firstName + ",</h2>"
             + "<p style=\"color:#444444;line-height:1.6;\">Voici votre code de vérification en deux étapes :</p>"
             + "<p style=\"font-size:32px;font-weight:700;letter-spacing:0.15em;color:#C42033;margin:16px 0;\">" + code + "</p>"
             + "<p style=\"color:#444444;line-height:1.6;\">Ce code est valable 10 minutes. Ne le partagez avec personne.</p>"
             + "<p style=\"color:#888888;font-size:13px;line-height:1.6;margin-top:24px;\">Si vous n'êtes pas à l'origine de cette demande, "
             + "ignorez simplement cet email.</p>"
             + "<p style=\"color:#444444;margin-top:24px;\">Cordialement,<br/>L'équipe SkillSet</p>"
             + "</div>";
    }

    public void sendAddedToTalentPoolEmail(String to, String firstName, String poolName, String companyName) {
        if (mailSender == null) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Vous avez été ajouté au vivier de talents de " + companyName);
        message.setText(
            "Bonjour " + firstName + ",\n\n" +
            "Vous avez été sélectionné pour rejoindre le vivier de talents \"" + poolName + "\" chez " + companyName + ".\n\n" +
            "Cordialement,\n" +
            "L'équipe de recrutement"
        );
        mailSender.send(message);
    }
}

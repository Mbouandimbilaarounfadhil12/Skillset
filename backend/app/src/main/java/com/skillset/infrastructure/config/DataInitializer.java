package com.skillset.infrastructure.config;

import com.skillset.domain.entity.Application;
import com.skillset.domain.entity.ApplicationStatus;
import com.skillset.domain.entity.CandidateProfile;
import com.skillset.domain.entity.EmployerProfile;
import com.skillset.domain.entity.JobListing;
import com.skillset.domain.entity.JobStatus;
import com.skillset.domain.entity.Message;
import com.skillset.domain.entity.User;
import com.skillset.domain.entity.UserRole;
import com.skillset.domain.port.ApplicationRepositoryPort;
import com.skillset.domain.port.CandidateProfileRepositoryPort;
import com.skillset.domain.port.EmployerProfileRepositoryPort;
import com.skillset.domain.port.JobRepositoryPort;
import com.skillset.domain.port.MessageRepositoryPort;
import com.skillset.domain.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepositoryPort userRepositoryPort;
    private final JobRepositoryPort jobRepositoryPort;
    private final EmployerProfileRepositoryPort employerProfileRepositoryPort;
    private final CandidateProfileRepositoryPort candidateProfileRepositoryPort;
    private final ApplicationRepositoryPort applicationRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;
    private final PasswordEncoder passwordEncoder;

    private static final String DEMO_PASSWORD = "Demo@SkillSet2026";

    // domaine | entreprise | ville | pays | intitulé du poste | compétences | salaire min | salaire max | type de contrat
    private static final String[][] SEED_JOBS = {
        {"Informatique & Développement logiciel", "TechNova Cameroun", "Douala", "Cameroun", "Développeur Full Stack", "React,Node.js,PostgreSQL", "450000", "800000", "CDI"},
        {"Informatique & Développement logiciel", "TechNova Cameroun", "Douala", "Cameroun", "Développeur Mobile Cross-Platform (Flutter / React Native)", "Flutter,Dart,Firebase", "400000", "700000", "CDI"},
        {"Intelligence Artificielle & Machine Learning", "DataMind Africa", "Yaoundé", "Cameroun", "Data Scientist", "Python,Machine Learning,Pandas", "500000", "900000", "CDI"},
        {"Intelligence Artificielle & Machine Learning", "DataMind Africa", "Yaoundé", "Cameroun", "Machine Learning Engineer", "Python,TensorFlow,MLOps", "550000", "950000", "CDI"},
        {"Cybersécurité", "CyberGuard CI", "Abidjan", "Côte d'Ivoire", "Analyste en cybersécurité (SOC)", "SIEM,Threat Detection,Réseaux", "450000", "800000", "CDI"},
        {"Cybersécurité", "CyberGuard CI", "Abidjan", "Côte d'Ivoire", "Pentesteur / Ethical Hacker", "Pentest,OWASP,Kali Linux", "500000", "900000", "Freelance"},
        {"Cloud Computing", "CloudAtlas SN", "Dakar", "Sénégal", "Cloud Engineer", "AWS,Terraform,Docker", "500000", "850000", "CDI"},
        {"Cloud Computing", "CloudAtlas SN", "Dakar", "Sénégal", "DevOps Engineer", "Kubernetes,CI/CD,Linux", "480000", "820000", "CDI"},
        {"E-commerce & Marketplaces", "MarketPlace237", "Douala", "Cameroun", "Responsable e-commerce", "Shopify,SEO,Analytics", "350000", "600000", "CDI"},
        {"E-commerce & Marketplaces", "MarketPlace237", "Douala", "Cameroun", "Traffic Manager", "SEA,Google Ads,Analytics", "300000", "500000", "CDI"},
        {"Fintech & Paiements digitaux", "PayFlex Africa", "Abidjan", "Côte d'Ivoire", "Product Manager Fintech", "Product Management,API,Agile", "500000", "900000", "CDI"},
        {"Fintech & Paiements digitaux", "PayFlex Africa", "Abidjan", "Côte d'Ivoire", "Ingénieur paiements (Stripe / Wave / MoMo)", "API bancaire,Java,Sécurité", "450000", "800000", "CDI"},
        {"Télécommunications", "TeleConnect CM", "Yaoundé", "Cameroun", "Ingénieur réseau télécom", "Cisco,MPLS,4G/5G", "400000", "700000", "CDI"},
        {"Télécommunications", "TeleConnect CM", "Yaoundé", "Cameroun", "Technicien fibre optique", "Fibre optique,FTTH,Maintenance", "250000", "450000", "CDI"},
        {"Banque & Services financiers", "Banque Panafricaine", "Douala", "Cameroun", "Chargé de clientèle Entreprises", "CRM,Négociation,Finance", "300000", "550000", "CDI"},
        {"Banque & Services financiers", "Banque Panafricaine", "Douala", "Cameroun", "Analyste crédit", "Analyse financière,Risque,Excel", "350000", "600000", "CDI"},
        {"Assurance", "AssurAfrique", "Dakar", "Sénégal", "Souscripteur", "Assurance,Analyse risque,Relation client", "280000", "500000", "CDI"},
        {"Assurance", "AssurAfrique", "Dakar", "Sénégal", "Chargé de sinistres", "Gestion sinistres,Droit assurance", "250000", "450000", "CDI"},
        {"Comptabilité & Audit", "FiduciaCompta", "Douala", "Cameroun", "Comptable général", "SAGE,OHADA,Excel", "250000", "450000", "CDI"},
        {"Comptabilité & Audit", "FiduciaCompta", "Douala", "Cameroun", "Auditeur interne", "Audit,IFRS,Risk management", "400000", "700000", "CDI"},
        {"Gestion d'actifs & Investissements", "CapitalGest", "Paris", "France", "Gestionnaire de portefeuille", "Finance,Bloomberg,Analyse", "45000", "75000", "CDI"},
        {"Gestion d'actifs & Investissements", "CapitalGest", "Paris", "France", "Analyste buy-side", "Modélisation financière,Excel", "40000", "65000", "CDI"},
        {"Santé & Médecine", "Clinique Espoir", "Yaoundé", "Cameroun", "Médecin généraliste", "Diagnostic,Soins,Relation patient", "400000", "800000", "CDI"},
        {"Santé & Médecine", "Clinique Espoir", "Yaoundé", "Cameroun", "Infirmier / Infirmière", "Soins infirmiers,Urgences", "150000", "300000", "CDI"},
        {"Pharmacie & Biotechnologies", "PharmaLab CM", "Douala", "Cameroun", "Pharmacien industriel", "Pharmacie,Contrôle qualité", "450000", "750000", "CDI"},
        {"Pharmacie & Biotechnologies", "PharmaLab CM", "Douala", "Cameroun", "Préparateur en pharmacie", "Délivrance,Gestion stock", "180000", "320000", "CDI"},
        {"Matériel médical", "MedEquip Afrique", "Abidjan", "Côte d'Ivoire", "Ingénieur biomédical", "Maintenance,Électronique médicale", "350000", "600000", "CDI"},
        {"Matériel médical", "MedEquip Afrique", "Abidjan", "Côte d'Ivoire", "Technicien de maintenance biomédicale", "Maintenance,Diagnostic", "250000", "450000", "CDI"},
        {"Éducation & Formation", "Groupe Scolaire Excellence", "Yaoundé", "Cameroun", "Enseignant (Primaire / Collège / Lycée)", "Pédagogie,Gestion de classe", "150000", "300000", "CDI"},
        {"Éducation & Formation", "Groupe Scolaire Excellence", "Yaoundé", "Cameroun", "Formateur professionnel", "Formation,Ingénierie pédagogique", "200000", "400000", "CDD"},
        {"E-learning & EdTech", "EduTech237", "Douala", "Cameroun", "Développeur plateforme LMS (Moodle / Canvas)", "Moodle,PHP,LMS", "350000", "600000", "CDI"},
        {"E-learning & EdTech", "EduTech237", "Douala", "Cameroun", "Instructional Designer", "Ingénierie pédagogique,Articulate", "250000", "450000", "CDI"},
        {"Recherche & Développement", "Institut R&D Afrique", "Dakar", "Sénégal", "Ingénieur R&D", "Recherche,Innovation,Rapport", "400000", "700000", "CDI"},
        {"Recherche & Développement", "Institut R&D Afrique", "Dakar", "Sénégal", "Chargé de veille technologique", "Veille,Analyse,Rédaction", "300000", "500000", "CDI"},
        {"Commerce de détail & Distribution", "Super U Cameroun", "Douala", "Cameroun", "Manager de magasin", "Gestion équipe,Merchandising", "300000", "500000", "CDI"},
        {"Commerce de détail & Distribution", "Super U Cameroun", "Douala", "Cameroun", "Chef de rayon", "Merchandising,Gestion stock", "200000", "350000", "CDI"},
        {"Vente & Marketing", "SalesForce Afrique", "Abidjan", "Côte d'Ivoire", "Business Developer", "Prospection,CRM,Négociation", "300000", "600000", "CDI"},
        {"Vente & Marketing", "SalesForce Afrique", "Abidjan", "Côte d'Ivoire", "Responsable marketing digital", "SEO,SEA,Analytics", "350000", "600000", "CDI"},
        {"Publicité & Communication", "AgenceCréative CM", "Douala", "Cameroun", "Social Media Manager", "Réseaux sociaux,Canva,Copywriting", "200000", "380000", "CDI"},
        {"Publicité & Communication", "AgenceCréative CM", "Douala", "Cameroun", "Graphiste", "Photoshop,Illustrator,InDesign", "180000", "350000", "CDI"},
        {"Industrie manufacturière", "IndustriPlus", "Douala", "Cameroun", "Ingénieur de production", "Production,Lean,Qualité", "400000", "700000", "CDI"},
        {"Industrie manufacturière", "IndustriPlus", "Douala", "Cameroun", "Responsable maintenance industrielle", "Maintenance,GMAO", "350000", "600000", "CDI"},
        {"Automobile & Mobilité", "AutoService CM", "Yaoundé", "Cameroun", "Mécanicien automobile", "Mécanique,Diagnostic,Réparation", "200000", "400000", "CDI"},
        {"Automobile & Mobilité", "AutoService CM", "Yaoundé", "Cameroun", "Technicien diagnostic automobile", "Diagnostic électronique,OBD", "220000", "420000", "CDI"},
        {"Aéronautique & Spatial", "AeroTech France", "Paris", "France", "Ingénieur aéronautique", "CAO,Aérodynamique,Matériaux", "42000", "70000", "CDI"},
        {"Aéronautique & Spatial", "AeroTech France", "Paris", "France", "Ingénieur systèmes embarqués", "C++,Systèmes embarqués,Temps réel", "40000", "65000", "CDI"},
        {"Chimie & Matériaux", "ChimiPro CM", "Douala", "Cameroun", "Ingénieur chimiste", "Chimie,Procédés,Laboratoire", "350000", "600000", "CDI"},
        {"Chimie & Matériaux", "ChimiPro CM", "Douala", "Cameroun", "Technicien de laboratoire chimique", "Analyse,Laboratoire", "200000", "380000", "CDI"},
        {"Textile & Mode", "ModaAfrica", "Abidjan", "Côte d'Ivoire", "Styliste / Designer mode", "Création,Patronage,Tendances", "250000", "450000", "CDI"},
        {"Textile & Mode", "ModaAfrica", "Abidjan", "Côte d'Ivoire", "Patron de couture / Modéliste", "Patronage,Couture,Textile", "200000", "380000", "CDI"},
        {"Construction & BTP", "BTP Solutions CM", "Douala", "Cameroun", "Conducteur de travaux", "Gestion chantier,Planification", "350000", "600000", "CDI"},
        {"Construction & BTP", "BTP Solutions CM", "Douala", "Cameroun", "Chef de chantier", "Coordination,Sécurité chantier", "280000", "480000", "CDI"},
        {"Immobilier", "ImmoConseil CM", "Yaoundé", "Cameroun", "Agent immobilier", "Négociation,Prospection,Droit immo", "200000", "450000", "Freelance"},
        {"Immobilier", "ImmoConseil CM", "Yaoundé", "Cameroun", "Gestionnaire de patrimoine immobilier", "Gestion locative,Finance", "300000", "500000", "CDI"},
        {"Architecture & Design", "Atelier Design CM", "Douala", "Cameroun", "Architecte", "AutoCAD,Revit,Conception", "400000", "700000", "CDI"},
        {"Architecture & Design", "Atelier Design CM", "Douala", "Cameroun", "Designer UX/UI", "Figma,UX Research,Prototypage", "300000", "550000", "CDI"},
        {"Ingénierie civile", "GenieCivil CI", "Abidjan", "Côte d'Ivoire", "Ingénieur génie civil", "Structures,AutoCAD,Calcul", "400000", "700000", "CDI"},
        {"Ingénierie civile", "GenieCivil CI", "Abidjan", "Côte d'Ivoire", "Ingénieur VRD (Voirie et réseaux divers)", "VRD,Topographie", "350000", "600000", "CDI"},
        {"Conseil & Management", "Conseil Stratégie Afrique", "Dakar", "Sénégal", "Consultant en stratégie", "Stratégie,Analyse,PowerPoint", "450000", "800000", "CDI"},
        {"Conseil & Management", "Conseil Stratégie Afrique", "Dakar", "Sénégal", "Consultant en transformation digitale", "Transformation digitale,Agile", "400000", "750000", "CDI"},
        {"Ressources Humaines", "RH Partners CM", "Douala", "Cameroun", "Chargé de recrutement", "Sourcing,Entretien,ATS", "250000", "450000", "CDI"},
        {"Ressources Humaines", "RH Partners CM", "Douala", "Cameroun", "Responsable RH", "GPEC,Droit du travail,Formation", "350000", "600000", "CDI"},
        {"Juridique & Droit", "Cabinet Juridique CM", "Yaoundé", "Cameroun", "Juriste d'entreprise", "Droit des affaires,Contrats", "350000", "600000", "CDI"},
        {"Juridique & Droit", "Cabinet Juridique CM", "Yaoundé", "Cameroun", "Avocat (droit des affaires)", "Droit des affaires,Plaidoirie", "400000", "750000", "Freelance"},
        {"Agriculture & Agroalimentaire", "AgroCam", "Bafoussam", "Cameroun", "Ingénieur agronome", "Agronomie,Production,Sols", "300000", "550000", "CDI"},
        {"Agriculture & Agroalimentaire", "AgroCam", "Bafoussam", "Cameroun", "Technicien agricole", "Agriculture,Terrain,Suivi cultures", "180000", "320000", "CDI"},
        {"Pêche & Aquaculture", "AquaPêche CM", "Douala", "Cameroun", "Technicien aquacole", "Aquaculture,Élevage poisson", "200000", "380000", "CDI"},
        {"Pêche & Aquaculture", "AquaPêche CM", "Douala", "Cameroun", "Responsable exploitation aquacole", "Gestion exploitation,Aquaculture", "280000", "480000", "CDI"},
        {"Énergie & Utilities", "EnerCam", "Douala", "Cameroun", "Ingénieur énergie", "Électricité,Réseaux,Efficacité énergétique", "400000", "700000", "CDI"},
        {"Énergie & Utilities", "EnerCam", "Douala", "Cameroun", "Technicien de maintenance centrale", "Maintenance électrique,Sécurité", "250000", "450000", "CDI"},
        {"Énergies renouvelables", "SolarAfrica", "Dakar", "Sénégal", "Ingénieur solaire (photovoltaïque)", "Photovoltaïque,Électricité,Dimensionnement", "400000", "700000", "CDI"},
        {"Énergies renouvelables", "SolarAfrica", "Dakar", "Sénégal", "Technicien installation panneaux solaires", "Installation solaire,Électricité", "220000", "400000", "CDI"},
        {"Pétrole & Gaz", "PetroGaz CM", "Douala", "Cameroun", "Ingénieur de réservoir", "Géologie,Réservoir,Simulation", "600000", "1000000", "CDI"},
        {"Pétrole & Gaz", "PetroGaz CM", "Douala", "Cameroun", "Ingénieur HSE pétrole & gaz", "HSE,Sécurité industrielle", "450000", "800000", "CDI"},
        {"Mines & Ressources naturelles", "MinesCam", "Garoua", "Cameroun", "Ingénieur des mines", "Exploitation minière,Géologie", "450000", "800000", "CDI"},
        {"Mines & Ressources naturelles", "MinesCam", "Garoua", "Cameroun", "Géologue minier", "Géologie,Cartographie,Terrain", "400000", "700000", "CDI"},
        {"Transport & Logistique", "LogiTrans CM", "Douala", "Cameroun", "Responsable logistique", "Supply Chain,Entrepôt,Planification", "350000", "600000", "CDI"},
        {"Transport & Logistique", "LogiTrans CM", "Douala", "Cameroun", "Chauffeur poids lourds (transport de marchandises)", "Permis poids lourd,Conduite", "180000", "320000", "CDI"},
        {"Maritime & Ports", "Port Autonome Douala", "Douala", "Cameroun", "Responsable exploitation portuaire", "Logistique portuaire,Douane", "400000", "700000", "CDI"},
        {"Maritime & Ports", "Port Autonome Douala", "Douala", "Cameroun", "Transitaire maritime", "Douane,Transit,Fret", "280000", "480000", "CDI"},
        {"Aviation", "AeroLignes CM", "Douala", "Cameroun", "Agent escale", "Service client,Opérations aéroportuaires", "200000", "350000", "CDI"},
        {"Aviation", "AeroLignes CM", "Douala", "Cameroun", "Responsable opérations sol", "Coordination,Sécurité aéroportuaire", "300000", "500000", "CDI"},
        {"Tourisme & Hôtellerie", "Hôtel Panorama", "Yaoundé", "Cameroun", "Directeur d'hôtel", "Gestion hôtelière,Management", "400000", "700000", "CDI"},
        {"Tourisme & Hôtellerie", "Hôtel Panorama", "Yaoundé", "Cameroun", "Responsable front office (réception)", "Accueil,Réservations,Anglais", "200000", "380000", "CDI"},
        {"Restauration", "Restaurant Le Gourmet", "Douala", "Cameroun", "Chef cuisinier", "Cuisine,Gestion brigade,Créativité", "250000", "450000", "CDI"},
        {"Restauration", "Restaurant Le Gourmet", "Douala", "Cameroun", "Responsable salle", "Service,Management équipe,Relation client", "150000", "280000", "CDI"},
        {"Événementiel", "EventPro CM", "Douala", "Cameroun", "Chef de projet événementiel", "Organisation,Logistique,Budget", "300000", "500000", "CDI"},
        {"Événementiel", "EventPro CM", "Douala", "Cameroun", "Wedding Planner", "Organisation,Créativité,Relation client", "200000", "400000", "Freelance"},
        {"Médias & Presse", "MediaGroup CM", "Yaoundé", "Cameroun", "Journaliste reporter", "Rédaction,Enquête,Interview", "200000", "400000", "CDI"},
        {"Médias & Presse", "MediaGroup CM", "Yaoundé", "Cameroun", "Rédacteur en chef", "Rédaction,Management éditorial", "350000", "600000", "CDI"},
        {"Production audiovisuelle", "StudioProd CM", "Douala", "Cameroun", "Monteur vidéo", "Premiere Pro,After Effects,Montage", "200000", "400000", "Freelance"},
        {"Production audiovisuelle", "StudioProd CM", "Douala", "Cameroun", "Chef opérateur / Cadreur", "Caméra,Lumière,Cadrage", "250000", "450000", "Freelance"},
        {"Arts & Culture", "Centre Culturel Afrique", "Dakar", "Sénégal", "Photographe professionnel", "Photographie,Retouche,Lightroom", "150000", "350000", "Freelance"},
        {"Arts & Culture", "Centre Culturel Afrique", "Dakar", "Sénégal", "Médiateur culturel", "Médiation,Communication,Événementiel", "200000", "380000", "CDI"},
        {"Sport & Loisirs", "SportClub CM", "Douala", "Cameroun", "Coach sportif / Entraîneur", "Coaching,Préparation physique", "150000", "300000", "CDI"},
        {"Sport & Loisirs", "SportClub CM", "Douala", "Cameroun", "Responsable de salle de sport / fitness", "Gestion,Coaching,Vente", "200000", "380000", "CDI"},
        {"ONG & Associations", "ONG Solidarité Afrique", "Yaoundé", "Cameroun", "Coordinateur de programme", "Gestion de projet,Reporting", "300000", "500000", "CDD"},
        {"ONG & Associations", "ONG Solidarité Afrique", "Yaoundé", "Cameroun", "Chargé de projet humanitaire", "Terrain,Logistique,Reporting bailleurs", "250000", "450000", "CDD"},
        {"Administration publique", "Mairie de Douala", "Douala", "Cameroun", "Attaché d'administration", "Administration,Gestion publique", "200000", "380000", "CDI"},
        {"Administration publique", "Mairie de Douala", "Douala", "Cameroun", "Chargé de mission administration", "Coordination,Rédaction administrative", "250000", "420000", "CDI"},
        {"Sécurité & Défense", "SecuriPro CM", "Douala", "Cameroun", "Agent de sécurité privée", "Surveillance,Vigilance,Rondes", "100000", "220000", "CDI"},
        {"Sécurité & Défense", "SecuriPro CM", "Douala", "Cameroun", "Responsable sécurité entreprise", "Sécurité,Gestion des risques", "300000", "500000", "CDI"},
        {"Artisanat & Métiers manuels", "Atelier Artisanal Bamenda", "Bamenda", "Cameroun", "Cordonnier / Réparateur de chaussures", "Cordonnerie,Réparation cuir,Couture", "100000", "220000", "Indépendant"},
        {"Artisanat & Métiers manuels", "Atelier Artisanal Bamenda", "Bamenda", "Cameroun", "Menuisier / Ébéniste", "Menuiserie,Travail du bois,Finition", "150000", "300000", "Indépendant"},
        {"Artisanat & Métiers manuels", "Atelier Artisanal Bamenda", "Bamenda", "Cameroun", "Soudeur / Chaudronnier", "Soudure,Métallurgie,Lecture de plans", "180000", "350000", "CDI"},
        {"Beauté & Bien-être", "Salon Belle Afrique", "Douala", "Cameroun", "Coiffeur / Coiffeuse", "Coiffure,Coloration,Relation client", "100000", "250000", "CDI"},
        {"Beauté & Bien-être", "Salon Belle Afrique", "Douala", "Cameroun", "Esthéticienne", "Soins esthétiques,Maquillage,Relation client", "120000", "260000", "CDI"},
        {"Boulangerie, Pâtisserie & Métiers de bouche", "Boulangerie Douala", "Douala", "Cameroun", "Boulanger", "Panification,Pétrissage,Cuisson", "120000", "250000", "CDI"},
        {"Boulangerie, Pâtisserie & Métiers de bouche", "Boulangerie Douala", "Douala", "Cameroun", "Pâtissier", "Pâtisserie,Décoration,Créativité", "130000", "270000", "CDI"},
        {"Nettoyage & Services à la personne", "CleanService CM", "Douala", "Cameroun", "Agent de nettoyage / Technicien de surface", "Nettoyage,Hygiène,Rigueur", "80000", "180000", "CDI"},
        {"Nettoyage & Services à la personne", "CleanService CM", "Douala", "Cameroun", "Aide à domicile", "Assistance,Bienveillance,Ménage", "90000", "190000", "CDI"},
        {"Autre", "SkillSet Services CM", "Douala", "Cameroun", "Assistant de direction", "Organisation,Gestion agenda,Communication", "200000", "380000", "CDI"},
        {"Autre", "SkillSet Services CM", "Douala", "Cameroun", "Office Manager", "Coordination,Administration,Gestion fournisseurs", "220000", "400000", "CDI"},
    };

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    @Override
    public void run(String... args) {
        createAdminIfAbsent(
            "Admin",
            "SkillSet",
            "admin@skillset.com",
            "Admin@SkillSet2026"
        );
        createAdminIfAbsent(
            "Super",
            "Admin",
            "superadmin@skillset.com",
            "Super@SkillSet2026"
        );
        seedJobsIfAbsent();
        seedDemoCandidateIfAbsent();
        seedApplicationPipelineIfAbsent();
        seedDemoConversationIfAbsent();
    }

    private void createAdminIfAbsent(String firstName, String lastName, String email, String rawPassword) {
        if (userRepositoryPort.existsByEmail(email)) {
            log.info("Admin account already exists: {}", email);
            return;
        }
        User admin = new User();
        admin.setFirstName(firstName);
        admin.setLastName(lastName);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setRole(UserRole.ADMIN);
        admin.setIsActive(true);
        admin.setTwoFactorEnabled(false);
        admin.setOnboardingCompleted(true);
        userRepositoryPort.saveUser(admin);
        log.info("Admin account created: {}", email);
    }

    private void seedJobsIfAbsent() {
        if (jobRepositoryPort.count() > 0) {
            log.info("Des offres existent déjà en base — seed d'offres ignoré ({} offres).", jobRepositoryPort.count());
            return;
        }

        Map<String, User> employersByCompany = new LinkedHashMap<>();
        int created = 0;

        for (String[] row : SEED_JOBS) {
            String domain = row[0];
            String company = row[1];
            String city = row[2];
            String country = row[3];
            String title = row[4];
            String skills = row[5];
            String salaryMin = row[6];
            String salaryMax = row[7];
            String jobType = row[8];

            User employer = employersByCompany.computeIfAbsent(company,
                c -> createEmployerIfAbsent(c, domain, city, country));

            JobListing job = new JobListing();
            job.setTitle(title);
            job.setDescription("Nous recherchons un(e) " + title + " pour rejoindre " + company
                + " (secteur " + domain + ") à " + city + ", " + country + ".");
            job.setCompanyId(employer.getId());
            job.setLocation(city + ", " + country);
            job.setJobType(jobType);
            job.setSalaryMin(salaryMin);
            job.setSalaryMax(salaryMax);
            job.setRequiredSkills(skills);
            job.setResponsibilities("Missions liées au poste de " + title + " au sein de " + company + ".");
            job.setStatus(JobStatus.OPEN);
            jobRepositoryPort.save(job);
            created++;
        }

        log.info("Seed terminé : {} entreprises et {} offres d'emploi créées sur {} domaines.",
            employersByCompany.size(), created, SEED_JOBS.length);
    }

    /**
     * Compte candidat de démo, profil déjà complété et volontairement calqué sur
     * la 1ère offre de TechNova Cameroun pour obtenir un score de matching élevé
     * dès la première connexion (utile pour les démos).
     */
    private void seedDemoCandidateIfAbsent() {
        String email = "candidat.demo@skillset.africa";
        if (userRepositoryPort.existsByEmail(email)) {
            log.info("Candidat de démo déjà présent : {}", email);
            return;
        }

        User candidate = new User();
        candidate.setFirstName("Amina");
        candidate.setLastName("Demo");
        candidate.setEmail(email);
        candidate.setPhoneNumber("+237600000001");
        candidate.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        candidate.setRole(UserRole.CANDIDATE);
        candidate.setIsActive(true);
        candidate.setTwoFactorEnabled(false);
        candidate.setOnboardingCompleted(true);
        User saved = userRepositoryPort.saveUser(candidate);

        CandidateProfile profile = new CandidateProfile();
        profile.setUserId(saved.getId());
        profile.setJobDomain("Informatique & Développement logiciel");
        profile.setDesiredRole("Développeur Full Stack");
        profile.setExperienceLevel("3 ans");
        profile.setContractType("CDI");
        profile.setCountry("Cameroun");
        profile.setCity("Douala");
        profile.setLocation("Douala, Cameroun");
        profile.setSkills("React, Node.js, PostgreSQL, TypeScript");
        profile.setBio("Développeuse Full Stack passionnée, spécialisée en écosystème JavaScript.");
        candidateProfileRepositoryPort.save(profile);

        log.info("Candidat de démo créé : {} (mot de passe : {})", email, DEMO_PASSWORD);
    }

    /**
     * Peuple le pipeline de candidatures de TechNova Cameroun (le compte recruteur de démo)
     * avec des candidatures réparties sur tous les statuts, pour que le Kanban recruteur et
     * la page "Mes candidatures" du candidat de démo soient déjà remplis à la démo.
     */
    private void seedApplicationPipelineIfAbsent() {
        var techNova = userRepositoryPort.findByEmail("technova-cameroun@demo.skillset.africa");
        var demoCandidate = userRepositoryPort.findByEmail("candidat.demo@skillset.africa");
        if (techNova.isEmpty() || demoCandidate.isEmpty()) {
            log.warn("Impossible de peupler le pipeline de candidatures : compte(s) de démo introuvable(s).");
            return;
        }
        if (!applicationRepositoryPort.findByJobSeekerId(demoCandidate.get().getId()).isEmpty()) {
            log.info("Le pipeline de candidatures de démo existe déjà — seed ignoré.");
            return;
        }

        List<JobListing> techNovaJobs = jobRepositoryPort.findByCompanyId(techNova.get().getId());
        JobListing fullStackJob = techNovaJobs.stream()
            .filter(j -> j.getTitle().contains("Full Stack")).findFirst().orElse(null);
        JobListing mobileJob = techNovaJobs.stream()
            .filter(j -> j.getTitle().contains("Mobile")).findFirst().orElse(null);
        if (fullStackJob == null || mobileJob == null) {
            log.warn("Offres TechNova introuvables — seed du pipeline de candidatures ignoré.");
            return;
        }

        User jeanPaul   = createFillerCandidateIfAbsent("Jean-Paul", "Fofana", "jeanpaul.demo@skillset.africa");
        User christelle = createFillerCandidateIfAbsent("Christelle", "Ngo", "christelle.demo@skillset.africa");
        User bruno      = createFillerCandidateIfAbsent("Bruno", "Kamdem", "bruno.demo@skillset.africa");
        User sandra     = createFillerCandidateIfAbsent("Sandra", "Ewome", "sandra.demo@skillset.africa");
        User rodrigue   = createFillerCandidateIfAbsent("Rodrigue", "Talla", "rodrigue.demo@skillset.africa");

        createApplication(demoCandidate.get(), fullStackJob, ApplicationStatus.APPROVED, 92.0, 8);
        createApplication(jeanPaul, fullStackJob, ApplicationStatus.INTERVIEW, 78.0, 6);
        createApplication(christelle, fullStackJob, ApplicationStatus.SCREENING, 65.0, 4);
        createApplication(bruno, mobileJob, ApplicationStatus.OFFER, 85.0, 5);
        createApplication(sandra, mobileJob, ApplicationStatus.SUBMITTED, 55.0, 1);
        createApplication(rodrigue, mobileJob, ApplicationStatus.REJECTED, 30.0, 10);

        log.info("Pipeline de candidatures de démo créé pour TechNova Cameroun (6 candidatures, tous statuts).");
    }

    private User createFillerCandidateIfAbsent(String firstName, String lastName, String email) {
        var existing = userRepositoryPort.findByEmail(email);
        if (existing.isPresent()) {
            return existing.get();
        }
        User candidate = new User();
        candidate.setFirstName(firstName);
        candidate.setLastName(lastName);
        candidate.setEmail(email);
        candidate.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        candidate.setRole(UserRole.CANDIDATE);
        candidate.setIsActive(true);
        candidate.setTwoFactorEnabled(false);
        candidate.setOnboardingCompleted(true);
        return userRepositoryPort.saveUser(candidate);
    }

    private void createApplication(User candidate, JobListing job, ApplicationStatus status,
                                    Double matchScore, long appliedDaysAgo) {
        Application application = new Application();
        application.setJobSeekerId(candidate.getId());
        application.setJobListing(job);
        application.setCoverLetter("Candidature générée pour la démo — motivé(e) par le poste de " + job.getTitle() + ".");
        application.setStatus(status);
        application.setMatchScore(matchScore);
        application.setMatchExplanation("Score calculé automatiquement à partir du profil candidat et des compétences requises.");
        application.setAppliedAt(LocalDateTime.now().minusDays(appliedDaysAgo));
        if (status == ApplicationStatus.APPROVED || status == ApplicationStatus.REJECTED) {
            application.setReviewedAt(LocalDateTime.now().minusDays(Math.max(0, appliedDaysAgo - 1)));
        }
        applicationRepositoryPort.save(application);
    }

    /**
     * Pré-remplit une conversation de démo entre le recruteur TechNova Cameroun et le
     * candidat de démo, pour vérifier/démontrer la messagerie sans avoir à écrire soi-même
     * le premier message.
     */
    private void seedDemoConversationIfAbsent() {
        var techNova = userRepositoryPort.findByEmail("technova-cameroun@demo.skillset.africa");
        var demoCandidate = userRepositoryPort.findByEmail("candidat.demo@skillset.africa");
        if (techNova.isEmpty() || demoCandidate.isEmpty()) {
            log.warn("Impossible de peupler la conversation de démo : compte(s) introuvable(s).");
            return;
        }

        boolean alreadyExists = !messageRepositoryPort
            .findBySenderIdAndRecipientId(techNova.get().getId(), demoCandidate.get().getId())
            .isEmpty();
        if (alreadyExists) {
            log.info("Conversation de démo déjà présente — seed ignoré.");
            return;
        }

        Message message = new Message();
        message.setSender(techNova.get());
        message.setRecipient(demoCandidate.get());
        message.setContent("Bonjour, félicitations pour votre candidature ! Quand êtes-vous disponible pour un entretien ?");
        message.setIsRead(false);
        message.setSentAt(LocalDateTime.now().minusHours(3));
        messageRepositoryPort.save(message);

        log.info("Conversation de démo créée entre TechNova Cameroun et le candidat de démo.");
    }

    private User createEmployerIfAbsent(String companyName, String industry, String city, String country) {
        String email = slugify(companyName) + "@demo.skillset.africa";
        var existing = userRepositoryPort.findByEmail(email);
        if (existing.isPresent()) {
            return existing.get();
        }

        User employer = new User();
        employer.setFirstName(companyName);
        employer.setLastName("RH");
        employer.setEmail(email);
        employer.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        employer.setRole(UserRole.EMPLOYER);
        employer.setIsActive(true);
        employer.setTwoFactorEnabled(false);
        employer.setOnboardingCompleted(true);
        User saved = userRepositoryPort.saveUser(employer);

        EmployerProfile profile = new EmployerProfile();
        profile.setUserId(saved.getId());
        profile.setCompanyName(companyName);
        profile.setIndustry(industry);
        profile.setCompanyCountry(country);
        profile.setCompanyCity(city);
        profile.setCompanySize("10-500");
        employerProfileRepositoryPort.save(profile);

        return saved;
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase();
        String slug = NON_ALNUM.matcher(normalized).replaceAll("-");
        return slug.replaceAll("^-+|-+$", "");
    }
}

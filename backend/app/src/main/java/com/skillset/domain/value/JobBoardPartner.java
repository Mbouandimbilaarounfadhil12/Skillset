package com.skillset.domain.value;

/**
 * Énumération des partenaires job board supportés.
 * Chaque partenaire a un adaptateur implémentant JobBoardAdapter.
 */
public enum JobBoardPartner {
    FRANCE_TRAVAIL("France Travail", "FR"),
    LINKEDIN("LinkedIn", "UNIVERSAL"),
    BRIGHTERMONDAY("BrighterMonday", "AFRICA"),
    JOBARTISAN("Jobartisan", "AFRICA");

    private final String displayName;
    private final String region;

    JobBoardPartner(String displayName, String region) {
        this.displayName = displayName;
        this.region = region;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRegion() {
        return region;
    }
}

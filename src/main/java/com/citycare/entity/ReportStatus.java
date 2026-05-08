package com.citycare.entity;

/**
 * ReportStatus - Ihbarin yasam dongusu durumlarini tanimlayan enum.
 *
 * Her durum iki ek alan tasir:
 *   displayName : Arayuzde gosterilecek Turkce durum adi.
 *   badgeClass  : Bootstrap badge renk sinifi (Bootstrap bg-* sinifi).
 *
 * Durum akisi:
 *   ACIK -> INCELEMEDE -> COZULDU
 *                      -> REDDEDILDI
 *
 * Durum gecisleri yalnizca admin tarafindan yapilabilir.
 */
public enum ReportStatus {
    ACIK("Acik", "warning"),
    INCELEMEDE("Incelemede", "info"),
    COZULDU("Cozuldu", "success"),
    REDDEDILDI("Reddedildi", "danger");

    private final String displayName;
    private final String badgeClass;

    ReportStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}

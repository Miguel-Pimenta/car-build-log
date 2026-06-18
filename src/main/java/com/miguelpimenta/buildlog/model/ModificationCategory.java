package com.miguelpimenta.buildlog.model;

/**
 * The kind of work a modification represents. Stored as a string in the
 * database (see {@code @Enumerated(EnumType.STRING)}) so ordering changes
 * here never corrupt existing rows.
 */
public enum ModificationCategory {
    ENGINE,
    EXHAUST,
    INTAKE,
    SUSPENSION,
    BRAKES,
    TUNING,
    COSMETIC,
    OTHER
}

package com.ekagra.model;

/** An achievement that can be unlocked (FR-34). The list of badges is seeded into the database. */
public class Badge {

    private final String code;
    private final String name;
    private final String description;
    private final String icon;

    public Badge(String code, String name, String description, String icon) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.icon = icon;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
}

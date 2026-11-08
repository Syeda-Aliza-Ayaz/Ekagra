package com.ekagra.model;

import java.time.LocalDateTime;

/**
 * Abstract base class for everyone who can log in.
 * INHERITANCE: {@link Student} and {@link Mentor} extend this class.
 * POLYMORPHISM: subclasses override {@link #getRole()}, {@link #getSubtitle()}, {@link #canManageRules()}.
 */
public abstract class User {

    private int id;
    private String fullName;
    private String email;
    private String passwordHash;
    private String salt;
    private String avatarColor = "#226192";
    private LocalDateTime createdAt = LocalDateTime.now();

    public abstract Role getRole();

    /** Short description shown under the user's name in the sidebar. */
    public abstract String getSubtitle();

    /** Whether this user may edit the global app-category rules. */
    public abstract boolean canManageRules();

    public String getInitials() {
        if (fullName == null || fullName.isBlank()) {
            return "?";
        }
        String[] parts = fullName.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }

    public String getFirstName() {
        return fullName == null ? "" : fullName.trim().split("\\s+")[0];
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public String getAvatarColor() { return avatarColor; }
    public void setAvatarColor(String avatarColor) { this.avatarColor = avatarColor; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** Two users are the same if they have the same database id. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof User other && id != 0 && id == other.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return getRole().getLabel() + "{" + id + ", " + fullName + ", " + email + "}";
    }
}

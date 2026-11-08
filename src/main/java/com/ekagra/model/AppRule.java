package com.ekagra.model;

import java.util.Locale;

/**
 * A classification rule, e.g. "chrome.exe + title contains YouTube = DISTRACTING".
 * A null processName means "any app"; a null titleKeyword means "any window of that app".
 */
public class AppRule {

    private int id;
    private String processName;
    private String titleKeyword;
    private Category category;
    private Integer createdBy;

    public AppRule() {
    }

    public AppRule(String processName, String titleKeyword, Category category) {
        this.processName = processName;
        this.titleKeyword = titleKeyword;
        this.category = category;
    }

    public boolean matches(String process, String title) {
        boolean processOk = processName == null
            || (process != null && processName.equalsIgnoreCase(process));
        boolean titleOk = titleKeyword == null
            || (title != null && title.toLowerCase(Locale.ROOT).contains(titleKeyword.toLowerCase(Locale.ROOT)));
        return processOk && titleOk;
    }

    /** Rules that check the window title are more specific than "whole app" rules. */
    public int specificity() {
        return (titleKeyword != null ? 2 : 0) + (processName != null ? 1 : 0);
    }

    public String describe() {
        if (processName != null && titleKeyword != null) {
            return processName + " with \"" + titleKeyword + "\" in title";
        }
        return processName != null ? processName : "Any app with \"" + titleKeyword + "\" in title";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }

    public String getTitleKeyword() { return titleKeyword; }
    public void setTitleKeyword(String titleKeyword) { this.titleKeyword = titleKeyword; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
}

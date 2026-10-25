
package com.thirtydegreesray.openhub.mvp.model;

/**
 * Form values submitted when creating or editing a repository.
 */
public class RepoEditInfo {

    public String name;
    public String description;
    public String homepage;
    public boolean isPrivate;
    /** only used in add mode, asks GitHub to create an initial commit with a README */
    public boolean autoInit;
    public boolean hasIssues = true;
    public boolean hasWiki = true;
    public boolean hasProjects = true;
    public boolean archived;

    public RepoEditInfo() {
    }

}

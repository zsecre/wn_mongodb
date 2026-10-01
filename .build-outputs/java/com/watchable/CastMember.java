package com.watchable;
import java.io.Serializable;
public class CastMember implements Serializable {
    private String name;
    private String profilePath;
    public CastMember() {}
    public String getName() { return name != null ? name : ""; }
    public void setName(String n) { this.name = n; }
    public String getProfilePath() { return profilePath; }
    public void setProfilePath(String p) { this.profilePath = p; }
}

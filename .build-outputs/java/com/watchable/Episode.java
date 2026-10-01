package com.watchable;
import java.io.Serializable;
public class Episode implements Serializable {
    private int episodeNumber;
    private int seasonNumber;
    private String name;
    private String stillPath;
    private String airDate;
    private String plot;
    public Episode() {}
    public int getEpisodeNumber() { return episodeNumber; }
    public void setEpisodeNumber(int e) { this.episodeNumber = e; }
    public int getSeasonNumber() { return seasonNumber; }
    public void setSeasonNumber(int s) { this.seasonNumber = s; }
    public String getName() { return name != null ? name : "Episode " + episodeNumber; }
    public void setName(String n) { this.name = n; }
    public String getStillPath() { return stillPath; }
    public void setStillPath(String s) { this.stillPath = s; }
    public String getAirDate() { return airDate != null ? airDate : "Coming Soon"; }
    public void setAirDate(String a) { this.airDate = a; }
    public String getPlot() { return plot != null ? plot : ""; }
    public void setPlot(String p) { this.plot = p; }
}

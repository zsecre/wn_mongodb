package com.watchable;

import java.io.Serializable;

public class MediaItem implements Serializable {
    private String id;
    private String title;
    private String posterPath;
    private String backdropPath;
    private double voteAverage;
    private String type;
    private String source;

    public MediaItem() {}

    public MediaItem(String id, String title, String posterPath, String backdropPath,
                     double voteAverage, String type, String source) {
        this.id = id; this.title = title; this.posterPath = posterPath;
        this.backdropPath = backdropPath; this.voteAverage = voteAverage;
        this.type = type; this.source = source;
    }

    public String getId() { return id != null ? id : ""; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title != null ? title : "Untitled"; }
    public void setTitle(String t) { this.title = t; }
    public String getPosterPath() { return posterPath != null ? posterPath : ""; }
    public void setPosterPath(String p) { this.posterPath = p; }
    public String getBackdropPath() {
        if (backdropPath != null && !backdropPath.isEmpty()) return backdropPath;
        return posterPath != null ? posterPath : "";
    }
    public void setBackdropPath(String b) { this.backdropPath = b; }
    public double getVoteAverage() { return voteAverage; }
    public void setVoteAverage(double v) { this.voteAverage = v; }
    public String getType() { return type != null ? type : "Movie"; }
    public void setType(String t) { this.type = t; }
    public String getSource() { return source != null ? source : "tmdb"; }
    public void setSource(String s) { this.source = s; }
    public String getRatingText() { return voteAverage == 0.0 ? "★ N/A" : String.format("★ %.1f", voteAverage); }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MediaItem)) return false;
        MediaItem m = (MediaItem) o;
        return id != null && id.equals(m.id) && type != null && type.equals(m.type);
    }
    public int hashCode() {
        return (id != null ? id.hashCode() : 0) * 31 + (type != null ? type.hashCode() : 0);
    }
}

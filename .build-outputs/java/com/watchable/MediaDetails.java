package com.watchable;

import java.io.Serializable;
import java.util.List;

public class MediaDetails extends MediaItem {
    private String overview;
    private String releaseDate;
    private String genres;
    private String status;
    private String director;
    private String originalLanguage;
    private Integer runtime;
    private List<Episode> episodes;
    private List<Integer> availableSeasons;
    private List<CastMember> cast;
    private List<MediaItem> similar;
    private String imdbId;

    public MediaDetails() {}
    public String getOverview() { return overview != null ? overview : "No overview available."; }
    public void setOverview(String o) { this.overview = o; }
    public String getReleaseDate() { return releaseDate != null ? releaseDate : "N/A"; }
    public void setReleaseDate(String r) { this.releaseDate = r; }
    public String getGenres() { return genres != null ? genres : "N/A"; }
    public void setGenres(String g) { this.genres = g; }
    public String getStatus() { return status != null ? status : "N/A"; }
    public void setStatus(String s) { this.status = s; }
    public String getDirector() { return director != null ? director : "N/A"; }
    public void setDirector(String d) { this.director = d; }
    public String getOriginalLanguage() { return originalLanguage != null ? originalLanguage : "en"; }
    public void setOriginalLanguage(String l) { this.originalLanguage = l; }
    public Integer getRuntime() { return runtime; }
    public void setRuntime(Integer r) { this.runtime = r; }
    public List<Episode> getEpisodes() { return episodes; }
    public void setEpisodes(List<Episode> e) { this.episodes = e; }
    public List<Integer> getAvailableSeasons() { return availableSeasons; }
    public void setAvailableSeasons(List<Integer> s) { this.availableSeasons = s; }
    public List<CastMember> getCast() { return cast; }
    public void setCast(List<CastMember> c) { this.cast = c; }
    public List<MediaItem> getSimilar() { return similar; }
    public void setSimilar(List<MediaItem> s) { this.similar = s; }
    public String getImdbId() { return imdbId; }
    public void setImdbId(String i) { this.imdbId = i; }
}

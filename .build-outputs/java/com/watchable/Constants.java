package com.watchable;

public class Constants {
    public static final String TMDB_KEY  = "3c6430e2495a6de8b81d7098ddb4c04c";
    public static final String TMDB_BASE = "https://api.themoviedb.org/3";
    public static final String VAL_KEY   = "9f4c2e81a7b3d6f0e5c8a1b4d7f2e9c3";
    public static final String JIKAN_BASE= "https://watchninja-malapi.val.run";
    public static final String IMDB_MAL_BASE = "https://watchninja-imdb.val.run";
    public static final String IMG_W500  = "https://image.tmdb.org/t/p/w500";
    public static final String IMG_ORI   = "https://image.tmdb.org/t/p/original";

    public static final String TYPE_MOVIE = "Movie";
    public static final String TYPE_TV    = "TV Show";
    public static final String TYPE_ANIME = "Anime";

    public static final String EXTRA_ITEM    = "extra_item";
    public static final String EXTRA_TITLE   = "extra_title";
    public static final String EXTRA_ITEMS   = "extra_items";
    public static final String EXTRA_GENRE   = "extra_genre";
    public static final String EXTRA_SEASON  = "extra_season";
    public static final String EXTRA_EPISODE = "extra_episode";
    
    // Database URL: Can be set to MongoDB REST API (e.g. https://your-watchninja-api.onrender.com)
    // or Firebase Realtime Database URL.
    public static final String DB_URL = "https://watchable-5a2ab-default-rtdb.asia-southeast1.firebasedatabase.app/";
    
    // Remote JSON Update URL
    public static final String APP_UPDATE_URL = DB_URL + (DB_URL.contains("firebasedatabase.app") ? "app_update.json" : "api/app_update");

    public static final String[] GENRES = {
        "Action","Adventure","Comedy","Drama","Fantasy",
        "Horror","Mystery","Romance","Sci-Fi","Thriller"
    };
}

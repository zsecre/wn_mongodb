package com.watchable;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.watchable.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MoviesFragment extends BaseTabFragment {
    private View root;
    private List<MediaItem> trendingMovies = new ArrayList<MediaItem>();
    private List<MediaItem> popularMovies  = new ArrayList<MediaItem>();
    private List<MediaItem> topRatedMovies = new ArrayList<MediaItem>();
    private List<MediaItem> animeMovies    = new ArrayList<MediaItem>();
    private List<MediaItem> topRatedAnime  = new ArrayList<MediaItem>();
    private List<MediaItem> slideshow      = new ArrayList<MediaItem>();

    @Nullable public View onCreateView(@NonNull LayoutInflater inf, ViewGroup c, Bundle s) {
        root = inf.inflate(R.layout.fragment_movies, c, false);
        setupSearchBtn(root); setupGenreChips(root); loadContent();
        return root;
    }

    private void loadContent() {
        ApiRepository api = ApiRepository.get();
        api.fetchTrendingMovies(new ApiRepository.Callback<List<MediaItem>>() {
            public void onResult(List<MediaItem> r) { trendingMovies.clear(); trendingMovies.addAll(r); rebuildSlideshow(); update(); }
            public void onError(Exception e) {}
        });
        api.fetchPopularMovies(new ApiRepository.Callback<List<MediaItem>>() {
            public void onResult(List<MediaItem> r) { popularMovies.clear(); popularMovies.addAll(r); update(); }
            public void onError(Exception e) {}
        });
        api.fetchTopRatedMovies(new ApiRepository.Callback<List<MediaItem>>() {
            public void onResult(List<MediaItem> r) { topRatedMovies.clear(); topRatedMovies.addAll(r); update(); }
            public void onError(Exception e) {}
        });
        api.fetchTopRatedAnimeMovies(new ApiRepository.Callback<List<MediaItem>>() {
            public void onResult(List<MediaItem> r) { topRatedAnime.clear(); topRatedAnime.addAll(r); update(); }
            public void onError(Exception e) {}
        });
        api.fetchLatestAnimeMovies(new ApiRepository.Callback<List<MediaItem>>() {
            public void onResult(List<MediaItem> r) { animeMovies.clear(); animeMovies.addAll(r); rebuildSlideshow(); update(); }
            public void onError(Exception e) {}
        });
    }

    private void rebuildSlideshow() {
        if (!isAdded() || root == null) return;
        slideshow.clear();
        List<MediaItem> mix = new ArrayList<MediaItem>();
        mix.addAll(trendingMovies.subList(0, Math.min(8, trendingMovies.size())));
        mix.addAll(animeMovies.subList(0, Math.min(7, animeMovies.size())));
        Collections.shuffle(mix);
        slideshow.addAll(mix.subList(0, Math.min(15, mix.size())));
        setupSlideshow(root, slideshow);
    }

    private void update() {
        if (!isAdded() || root == null) return;
        setupMediaRow(root, R.id.row_trending_movies, "Trending Movies",          trendingMovies, trendingMovies, "Trending Movies");
        setupMediaRow(root, R.id.row_anime_movies,    "Latest Anime Movies",      animeMovies,    animeMovies,    "Latest Anime Movies");
        setupMediaRow(root, R.id.row_popular_movies,  "Popular Movies",           popularMovies,  popularMovies,  "Popular Movies");
        setupMediaRow(root, R.id.row_top_movies,      "Top Rated Movies",         topRatedMovies, topRatedMovies, "Top Rated Movies");
        setupMediaRow(root, R.id.row_top_anime,       "Top Rated Anime Movies",   topRatedAnime,  topRatedAnime,  "Top Rated Anime Movies");
    }
}

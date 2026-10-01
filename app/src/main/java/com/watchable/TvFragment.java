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

public class TvFragment extends BaseTabFragment {
    private View root;
    private List<MediaItem> trendingTv  = new ArrayList<MediaItem>();
    private List<MediaItem> popularTv   = new ArrayList<MediaItem>();
    private List<MediaItem> topRatedTv  = new ArrayList<MediaItem>();
    private List<MediaItem> animeSeries = new ArrayList<MediaItem>();
    private List<MediaItem> topRatedAnime = new ArrayList<MediaItem>();
    private List<MediaItem> slideshow   = new ArrayList<MediaItem>();

    @Nullable public View onCreateView(@NonNull LayoutInflater inf, ViewGroup c, Bundle s) {
        root = inf.inflate(R.layout.fragment_tv, c, false);
        setupSearchBtn(root); setupGenreChips(root); loadContent();
        return root;
    }

    private void loadContent() {
        ApiRepository api = ApiRepository.get();
        api.fetchTrendingTv(new ApiRepository.Callback<List<MediaItem>>() {
            public void onResult(List<MediaItem> r) { trendingTv.clear(); trendingTv.addAll(r); rebuildSlideshow(); update(); }
            public void onError(Exception e) {}
        });
        api.fetchPopularTv(new ApiRepository.Callback<List<MediaItem>>() {
            public void onResult(List<MediaItem> r) { popularTv.clear(); popularTv.addAll(r); rebuildSlideshow(); update(); }
            public void onError(Exception e) {}
        });
        api.fetchTopRatedTv(new ApiRepository.Callback<List<MediaItem>>() {
            public void onResult(List<MediaItem> r) { topRatedTv.clear(); topRatedTv.addAll(r); update(); }
            public void onError(Exception e) {}
        });
        api.fetchTopRatedAnimeSeries(new ApiRepository.Callback<List<MediaItem>>() {
            public void onResult(List<MediaItem> r) { topRatedAnime.clear(); topRatedAnime.addAll(r); update(); }
            public void onError(Exception e) {}
        });
        api.fetchLatestAnimeSeries(new ApiRepository.Callback<List<MediaItem>>() {
            public void onResult(List<MediaItem> r) { animeSeries.clear(); animeSeries.addAll(r); rebuildSlideshow(); update(); }
            public void onError(Exception e) {}
        });
    }

    private void rebuildSlideshow() {
        if (!isAdded() || root == null) return;
        slideshow.clear();
        List<MediaItem> mix = new ArrayList<MediaItem>();
        mix.addAll(popularTv.subList(0, Math.min(7, popularTv.size())));
        mix.addAll(animeSeries.subList(0, Math.min(8, animeSeries.size())));
        Collections.shuffle(mix);
        slideshow.addAll(mix.subList(0, Math.min(15, mix.size())));
        setupSlideshow(root, slideshow);
    }

    private void update() {
        if (!isAdded() || root == null) return;
        setupMediaRow(root, R.id.row_trending_tv,  "Trending TV Shows",   trendingTv,  trendingTv,  "Trending TV Shows");
        setupMediaRow(root, R.id.row_anime_series, "Latest Anime Series", animeSeries, animeSeries, "Latest Anime Series");
        setupMediaRow(root, R.id.row_popular_tv,   "Popular TV Shows",    popularTv,   popularTv,   "Popular TV Shows");
        setupMediaRow(root, R.id.row_top_tv,       "Top Rated TV Shows",  topRatedTv,  topRatedTv,  "Top Rated TV Shows");
        setupMediaRow(root, R.id.row_top_anime,    "Top Rated Anime Series", topRatedAnime, topRatedAnime, "Top Rated Anime Series");
    }
}

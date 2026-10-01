package com.watchable;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.watchable.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends BaseTabFragment {
    private View root;
    private List<MediaItem> slideshowItems = new ArrayList<MediaItem>();
    private List<MediaItem> trendingMovies = new ArrayList<MediaItem>();
    private List<MediaItem> trendingTv     = new ArrayList<MediaItem>();
    private List<MediaItem> latestAnime    = new ArrayList<MediaItem>();
    private List<MediaItem> popular        = new ArrayList<MediaItem>();
    private List<MediaItem> popularAnime   = new ArrayList<MediaItem>();
    private List<MediaItem> topRatedMovies = new ArrayList<MediaItem>();
    private List<MediaItem> topRatedTv     = new ArrayList<MediaItem>();
    private List<MediaItem> topRatedAnime  = new ArrayList<MediaItem>();

    @Nullable public View onCreateView(@NonNull LayoutInflater inf, ViewGroup c, Bundle s) {
        root = inf.inflate(R.layout.fragment_home, c, false);
        setupSearchBtn(root); setupGenreChips(root); setupSeasonalAnime(root); loadContent();
        return root;
    }
    
    private void setupSeasonalAnime(View root) {
        final android.widget.Spinner spinner = root.findViewById(R.id.spinner_year);
        if (spinner != null) {
            java.util.List<String> years = new java.util.ArrayList<>();
            int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
            for (int i = currentYear + 1; i >= 1990; i--) years.add(String.valueOf(i));
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, years);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setSelection(1); // Set current year as default
        }

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (spinner == null) return;
                String yearStr = (String) spinner.getSelectedItem();
                int year = Integer.parseInt(yearStr);
                String season = "";
                int id = v.getId();
                if (id == R.id.card_summer) season = "summer";
                else if (id == R.id.card_fall) season = "fall";
                else if (id == R.id.card_winter) season = "winter";
                else if (id == R.id.card_spring) season = "spring";
                
                if (season.isEmpty()) return;
                
                final String finalSeason = season;
                android.widget.Toast.makeText(getContext(), "Loading " + yearStr + " " + season + " anime...", android.widget.Toast.LENGTH_SHORT).show();
                
                ApiRepository.get().fetchSeasonalAnime(year, season, new ApiRepository.Callback<List<MediaItem>>() {
                    @Override
                    public void onResult(List<MediaItem> r) {
                        if (r != null && !r.isEmpty()) {
                            String capSeason = finalSeason.substring(0, 1).toUpperCase() + finalSeason.substring(1);
                            openViewAll(yearStr + " " + capSeason + " Anime", r);
                        } else {
                            android.widget.Toast.makeText(getContext(), "No anime found", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onError(Exception e) {
                        android.widget.Toast.makeText(getContext(), "Error loading", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };

        View cardSummer = root.findViewById(R.id.card_summer);
        View cardFall = root.findViewById(R.id.card_fall);
        View cardWinter = root.findViewById(R.id.card_winter);
        
        View cardSpring = root.findViewById(R.id.card_spring);
        
        ImageView imgWinter = root.findViewById(R.id.img_winter);
        ImageView imgSpring = root.findViewById(R.id.img_spring);
        ImageView imgSummer = root.findViewById(R.id.img_summer);
        ImageView imgFall = root.findViewById(R.id.img_fall);
        
        if (imgWinter != null) com.bumptech.glide.Glide.with(this).load("https://raw.githubusercontent.com/zsecre/Watchable/refs/heads/main/Winter.png").into(imgWinter);
        if (imgSpring != null) com.bumptech.glide.Glide.with(this).load("https://raw.githubusercontent.com/zsecre/Watchable/refs/heads/main/Spring.png").into(imgSpring);
        if (imgSummer != null) com.bumptech.glide.Glide.with(this).load("https://raw.githubusercontent.com/zsecre/Watchable/refs/heads/main/Summer.png").into(imgSummer);
        if (imgFall != null) com.bumptech.glide.Glide.with(this).load("https://raw.githubusercontent.com/zsecre/Watchable/refs/heads/main/Fall.png").into(imgFall);

        if (cardSummer != null) cardSummer.setOnClickListener(listener);
        if (cardFall != null) cardFall.setOnClickListener(listener);
        if (cardWinter != null) cardWinter.setOnClickListener(listener);
        if (cardSpring != null) cardSpring.setOnClickListener(listener);
    }

    private void loadContent() {
        ApiRepository api = ApiRepository.get();
        api.fetchTrendingMovies(new ApiRepository.Callback<List<MediaItem>>() {
            public void onResult(List<MediaItem> r) { trendingMovies.clear(); trendingMovies.addAll(r); rebuildSlideshow(); updateRows(); }
            public void onError(Exception e) {}
        });
        api.fetchTrendingTv(new ApiRepository.Callback<List<MediaItem>>() {
            public void onResult(List<MediaItem> r) { trendingTv.clear(); trendingTv.addAll(r); rebuildSlideshow(); updateRows(); }
            public void onError(Exception e) {}
        });
        api.fetchLatestAnime(new ApiRepository.Callback<List<MediaItem>>() {
            public void onResult(List<MediaItem> r) { latestAnime.clear(); latestAnime.addAll(r); rebuildSlideshow(); updateRows(); }
            public void onError(Exception e) {}
        });
        api.fetchPopularMovies(new ApiRepository.Callback<List<MediaItem>>() {
            public void onResult(List<MediaItem> r) { popular.clear(); popular.addAll(r); updateRows(); }
            public void onError(Exception e) {}
        });
        api.fetchPopularAnime(new ApiRepository.Callback<List<MediaItem>>() {
            public void onResult(List<MediaItem> r) { popularAnime.clear(); popularAnime.addAll(r); updateRows(); }
            public void onError(Exception e) {}
        });
        api.fetchTopRatedMovies(new ApiRepository.Callback<List<MediaItem>>() {
            public void onResult(List<MediaItem> r) { topRatedMovies.clear(); topRatedMovies.addAll(r); updateRows(); }
            public void onError(Exception e) {}
        });
        api.fetchTopRatedTv(new ApiRepository.Callback<List<MediaItem>>() {
            public void onResult(List<MediaItem> r) { topRatedTv.clear(); topRatedTv.addAll(r); updateRows(); }
            public void onError(Exception e) {}
        });
        api.fetchTopRatedAnime(new ApiRepository.Callback<List<MediaItem>>() {
            public void onResult(List<MediaItem> r) { topRatedAnime.clear(); topRatedAnime.addAll(r); updateRows(); }
            public void onError(Exception e) {}
        });
    }

    private void rebuildSlideshow() {
        if (!isAdded() || root == null) return;
        slideshowItems.clear();
        List<MediaItem> mix = new ArrayList<MediaItem>();
        mix.addAll(trendingMovies.subList(0, Math.min(5, trendingMovies.size())));
        mix.addAll(trendingTv.subList(0, Math.min(5, trendingTv.size())));
        mix.addAll(latestAnime.subList(0, Math.min(5, latestAnime.size())));
        Collections.shuffle(mix);
        slideshowItems.addAll(mix.subList(0, Math.min(12, mix.size())));
        setupSlideshow(root, slideshowItems);
    }

    private void updateRows() {
        if (!isAdded() || root == null) return;
        setupMediaRow(root, R.id.row_latest_anime,    "Latest Anime",           latestAnime,    latestAnime,    "Latest Anime");
        setupMediaRow(root, R.id.row_trending_movies, "Trending Movies",        trendingMovies, trendingMovies, "Trending Movies");
        setupMediaRow(root, R.id.row_trending_tv,     "Trending TV Shows",      trendingTv,     trendingTv,     "Trending TV Shows");
        setupMediaRow(root, R.id.row_popular,         "Popular",                popular,        popular,        "Popular");
        setupMediaRow(root, R.id.row_popular_anime,   "All Time Popular Anime", popularAnime,   popularAnime,   "All Time Popular Anime");
        setupMediaRow(root, R.id.row_top_movies,      "Top Rated Movies",       topRatedMovies, topRatedMovies, "Top Rated Movies");
        setupMediaRow(root, R.id.row_top_tv,          "Top Rated TV Shows",     topRatedTv,     topRatedTv,     "Top Rated TV Shows");
        setupMediaRow(root, R.id.row_top_anime,       "Top Rated Anime",        topRatedAnime,  topRatedAnime,  "Top Rated Anime");
    }
}

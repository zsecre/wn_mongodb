package com.watchable;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.watchable.R;
import java.util.ArrayList;
import java.util.List;

public class DetailsActivity extends AppCompatActivity {
    private MediaItem item;
    private MediaDetails details;
    private AppPreferences prefs;
    private ImageButton bookmarkBtn;
    private EpisodeAdapter episodeAdapter;
    private int currentSeason = 1;
    private String imdbId = null;

    private ImageView backdropImg, posterImg;
    private TextView titleTv, typeBadge, ratingTv, overviewTv;
    private TextView detailDirector, detailRelease, detailGenres, detailLang, detailRuntime, detailStatus;
    private TextView detailExclusiveViews, detailExclusiveRatings, detailExclusiveLikes, detailExclusiveDislikes;
    private LinearLayout episodesSection, castSection, similarSection, seasonsRow;
    private RecyclerView episodeRv, castRv, similarRv;
    private ProgressBar loadingSpinner;
    private View episodesLoadingContainer;
    private ImageView episodesLoadingGif;
    private TextView episodesLoadingText;
    private android.widget.EditText detailEpisodeInput;
    private Button detailEpisodeGo;
    private org.json.JSONArray animeSeasonsMapping = null;
    private boolean isAnime = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            item = (MediaItem) getIntent().getSerializableExtra(Constants.EXTRA_ITEM);
            if (item == null) { finish(); return; }
            prefs = AppPreferences.get(this);
            setContentView(R.layout.activity_details);
            bindViews();
            populateBasicInfo();
            resolveAnimeMappingAndStats();
            fetchDetails();
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }

    private void bindViews() {
        backdropImg     = findViewById(R.id.details_backdrop);
        posterImg       = findViewById(R.id.details_poster);
        titleTv         = findViewById(R.id.details_title);
        typeBadge       = findViewById(R.id.details_type_badge);
        ratingTv        = findViewById(R.id.details_rating);
        overviewTv      = findViewById(R.id.details_overview);
        detailDirector  = findViewById(R.id.detail_director);
        detailRelease   = findViewById(R.id.detail_release);
        detailGenres    = findViewById(R.id.detail_genres);
        detailLang      = findViewById(R.id.detail_language);
        detailRuntime   = findViewById(R.id.detail_runtime);
        detailStatus    = findViewById(R.id.detail_status);
        detailExclusiveViews    = findViewById(R.id.detail_exclusive_views);
        detailExclusiveRatings  = findViewById(R.id.detail_exclusive_ratings);
        detailExclusiveLikes    = findViewById(R.id.detail_exclusive_likes);
        detailExclusiveDislikes = findViewById(R.id.detail_exclusive_dislikes);
        episodesSection = findViewById(R.id.episodes_section);
        castSection     = findViewById(R.id.cast_section);
        similarSection  = findViewById(R.id.similar_section);
        seasonsRow      = findViewById(R.id.seasons_row);
        episodeRv       = findViewById(R.id.episodes_rv);
        castRv          = findViewById(R.id.cast_rv);
        similarRv       = findViewById(R.id.similar_rv);
        loadingSpinner  = findViewById(R.id.details_loading);
        bookmarkBtn     = findViewById(R.id.bookmark_btn);
        episodesLoadingContainer = findViewById(R.id.episodes_loading_container);
        episodesLoadingGif       = findViewById(R.id.episodes_loading_gif);
        episodesLoadingText      = findViewById(R.id.episodes_loading_text);
        detailEpisodeInput = findViewById(R.id.details_episode_input);
        detailEpisodeGo = findViewById(R.id.details_episode_go);
        
        detailEpisodeGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String val = detailEpisodeInput.getText().toString();
                if (!val.isEmpty()) {
                    try {
                        int pos = Integer.parseInt(val) - 1;
                        if (episodeAdapter != null && pos >= 0 && pos < episodeAdapter.getItemCount()) {
                            episodeRv.scrollToPosition(pos);
                        } else if (pos >= 0) {
                            startPlaying(currentSeason, pos + 1);
                        }
                    } catch (Exception e){}
                }
            }
        });

        episodesSection.setVisibility(View.GONE);
        castSection.setVisibility(View.GONE);
        similarSection.setVisibility(View.GONE);

        findViewById(R.id.details_back_btn).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        updateBookmarkIcon();
        bookmarkBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { prefs.toggleWatchlist(item); updateBookmarkIcon(); }
        });
        Button playBtn = findViewById(R.id.play_btn);
        if ("TV Show".equals(item.getType()) || "Anime".equals(item.getType())) {
            playBtn.setText("Start Watching Ep-1");
        } else {
            playBtn.setText("Watch Now");
        }
        playBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startPlaying(1, 1); }
        });

        View statsInfoLayout = findViewById(R.id.layout_stats_info);
        if (statsInfoLayout != null) {
            final String statsMsg = "The show statistics below is only recorded here in WatchNinja app since the app published in September 2026. Overall views, likes, ratings and etc. from other sources are not included below.";
            androidx.core.view.ViewCompat.setTooltipText(statsInfoLayout, statsMsg);
            statsInfoLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new androidx.appcompat.app.AlertDialog.Builder(DetailsActivity.this)
                        .setTitle("WatchNinja Statistics")
                        .setMessage(statsMsg)
                        .setPositiveButton("Got it", null)
                        .show();
                }
            });
        }
    }

    private void populateBasicInfo() {
        titleTv.setText(item.getTitle());
        typeBadge.setText(item.getType());
        ratingTv.setText(item.getRatingText());
        Glide.with(this).load(item.getBackdropPath()).error(R.drawable.placeholder_backdrop).centerCrop().into(backdropImg);
        Glide.with(this).load(item.getPosterPath()).error(R.drawable.placeholder_poster).centerCrop().into(posterImg);
    }

    private void populateExclusiveStats(MediaDetails d) {
        final List<String> targetIds = new ArrayList<>();
        if (d != null) {
            if (d.getId() != null && !d.getId().isEmpty()) targetIds.add(d.getId());
            if (d.getImdbId() != null && !d.getImdbId().isEmpty() && !targetIds.contains(d.getImdbId())) targetIds.add(d.getImdbId());
        }
        if (item != null) {
            if (item.getId() != null && !item.getId().isEmpty() && !targetIds.contains(item.getId())) targetIds.add(item.getId());
        }
        if (imdbId != null && !imdbId.isEmpty() && !targetIds.contains(imdbId)) {
            targetIds.add(imdbId);
        }
        if (animeSeasonsMapping != null) {
            for (int i = 0; i < animeSeasonsMapping.length(); i++) {
                org.json.JSONObject obj = animeSeasonsMapping.optJSONObject(i);
                if (obj != null) {
                    String mId = obj.optString("mal_id");
                    if (mId != null && !mId.isEmpty() && !mId.equals("null") && !targetIds.contains(mId)) {
                        targetIds.add(mId);
                    }
                }
            }
        }
        
        if (targetIds.isEmpty()) return;

        DatabaseService.get().fetchBatchStats(targetIds, new DatabaseService.DataCallback<DatabaseService.StatsData>() {
            @Override
            public void onSuccess(DatabaseService.StatsData data) {
                if (detailExclusiveViews != null) detailExclusiveViews.setText(formatNumber(data.views));
                if (detailExclusiveRatings != null) {
                    detailExclusiveRatings.setText(data.ratingAverage > 0 ? String.format(java.util.Locale.US, "%.1f", data.ratingAverage) : "N/A");
                }
                if (detailExclusiveLikes != null) detailExclusiveLikes.setText(formatNumber(data.likes));
                if (detailExclusiveDislikes != null) detailExclusiveDislikes.setText(formatNumber(data.dislikes));
            }

            @Override
            public void onError(Exception e) {}
        });
    }

    private void resolveAnimeMappingAndStats() {
        if (imdbId != null && !imdbId.isEmpty()) {
            fetchAnimeMapping();
        } else if ("jikan".equals(item.getSource()) || "Anime".equals(item.getType())) {
            new Thread(() -> {
                try {
                    java.net.URL url = new java.net.URL("https://arm.haglund.dev/api/v2/ids?source=myanimelist&id=" + item.getId());
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    java.io.InputStream is = conn.getInputStream();
                    java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                    int nRead;
                    byte[] data = new byte[1024];
                    while ((nRead = is.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    String json = new String(buffer.toByteArray(), "UTF-8");
                    org.json.JSONObject obj = new org.json.JSONObject(json);
                    if (obj.has("imdb") && !obj.isNull("imdb")) {
                        imdbId = obj.getString("imdb");
                        fetchAnimeMapping();
                    } else {
                        populateExclusiveStats(details != null ? details : null);
                    }
                } catch (Exception e) {
                    populateExclusiveStats(details != null ? details : null);
                }
            }).start();
        } else {
            populateExclusiveStats(details != null ? details : null);
        }
    }
    
    private String formatNumber(long num) {
        if (num >= 1000000) return String.format(java.util.Locale.US, "%.1fM", num / 1000000.0);
        if (num >= 1000) return String.format(java.util.Locale.US, "%.1fK", num / 1000.0);
        return String.valueOf(num);
    }

    private void fetchDetails() {
        loadingSpinner.setVisibility(View.VISIBLE);
        ApiRepository.get().fetchDetails(item, new ApiRepository.Callback<MediaDetails>() {
            public void onResult(MediaDetails d) {
                details = d;
                loadingSpinner.setVisibility(View.GONE);
                populateDetails(d);
            }
            public void onError(Exception e) {
                loadingSpinner.setVisibility(View.GONE);
                overviewTv.setText("Could not load details.");
            }
        });
    }

    private void populateDetails(final MediaDetails d) {
        try {
            titleTv.setText(d.getTitle());
            ratingTv.setText(d.getRatingText());
            overviewTv.setText(d.getOverview());
            if (d.getPosterPath() != null && !d.getPosterPath().isEmpty())
                Glide.with(this).load(d.getPosterPath()).error(R.drawable.placeholder_poster).centerCrop().into(posterImg);
            if (d.getBackdropPath() != null && !d.getBackdropPath().isEmpty())
                Glide.with(this).load(d.getBackdropPath()).error(R.drawable.placeholder_backdrop).centerCrop().into(backdropImg);

            detailDirector.setText(d.getDirector());
            detailRelease.setText(d.getReleaseDate());
            detailGenres.setText(d.getGenres());
            detailLang.setText(d.getOriginalLanguage().toUpperCase());
            detailRuntime.setText(d.getRuntime() != null ? d.getRuntime() + " mins" : "N/A");
            detailStatus.setText(d.getStatus());
            if (d.getImdbId() != null && !d.getImdbId().isEmpty()) imdbId = d.getImdbId();
            resolveAnimeMappingAndStats();

            if (d.getCast() != null && !d.getCast().isEmpty()) {
                castSection.setVisibility(View.VISIBLE);
                castRv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                castRv.setAdapter(new CastAdapter(d.getCast()));
            }
            if (d.getSimilar() != null && !d.getSimilar().isEmpty()) {
                similarSection.setVisibility(View.VISIBLE);
                similarRv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                similarRv.setAdapter(new MediaRowAdapter(d.getSimilar(), new MediaRowAdapter.OnItemClick() {
                    public void onClick(MediaItem other) { openDetails(other); }
                }));
            }
            if ("TV Show".equals(item.getType()) || "Anime".equals(item.getType())) {
                episodesSection.setVisibility(View.VISIBLE);
                if (episodeAdapter == null) {
                    episodeAdapter = new EpisodeAdapter(new EpisodeAdapter.OnClick() {
                        public void onClick(Episode ep) { startPlaying(currentSeason, ep.getEpisodeNumber()); }
                    });
                    episodeAdapter.setPosterPath(item.getPosterPath());
                    episodeRv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                    episodeRv.setAdapter(episodeAdapter);
                }
                isAnime = "Anime".equals(item.getType());
                if (imdbId != null && !imdbId.isEmpty()) {
                    fetchAnimeMapping();
                } else if (d.getAvailableSeasons() != null && !d.getAvailableSeasons().isEmpty()) {
                    setupSeasons(d.getAvailableSeasons(), null);
                } else if (episodeAdapter.getItemCount() == 0) {
                    loadEpisodes(currentSeason);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupSeasons(List<Integer> seasons, List<String> seasonNames) {
        if (seasons == null || seasons.isEmpty()) return;
        seasonsRow.setVisibility(View.VISIBLE);
        if (seasonsRow.getChildCount() != seasons.size() || seasonNames != null) {
            buildSeasonButtons(seasons, seasonNames);
            if (!seasons.contains(currentSeason)) {
                currentSeason = seasons.get(0);
            }
            loadEpisodes(currentSeason);
        }
    }

    private String formatAnimeTitle(String id) {
        if (id == null || id.isEmpty()) return null;
        String[] parts = id.split("-");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.length() > 0) {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private void buildSeasonButtons(List<Integer> seasons, List<String> seasonNames) {
        seasonsRow.removeAllViews();
        for (int i = 0; i < seasons.size(); i++) {
            final int sn = seasons.get(i);
            Button btn = new Button(this);
            btn.setText(seasonNames != null && i < seasonNames.size() && seasonNames.get(i) != null ? seasonNames.get(i) : "Season " + sn);
            btn.setTextSize(11f);
            btn.setAllCaps(false);
            updateSeasonBtnStyle(btn, sn == currentSeason);
            btn.setTag(sn);
            btn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    currentSeason = sn;
                    for (int i = 0; i < seasonsRow.getChildCount(); i++) {
                        View child = seasonsRow.getChildAt(i);
                        if (child instanceof Button)
                            updateSeasonBtnStyle((Button) child, (int) child.getTag() == sn);
                    }
                    loadEpisodes(sn);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(8);
            btn.setLayoutParams(lp);
            seasonsRow.addView(btn);
        }
    }

    private void updateSeasonBtnStyle(Button btn, boolean active) {
        btn.setBackgroundResource(active ? R.drawable.bg_season_btn_active : R.drawable.bg_season_btn);
        btn.setTextColor(active ? 0xFF000000 : 0xFFD1D5DB);
    }

    private void showEpisodesLoading(final boolean show) {
        runOnUiThread(() -> {
            if (episodesLoadingContainer != null) {
                episodesLoadingContainer.setVisibility(show ? View.VISIBLE : View.GONE);
                if (show && episodesLoadingGif != null) {
                    try {
                        Glide.with(DetailsActivity.this)
                            .asGif()
                            .load("https://raw.githubusercontent.com/zsecre/Watchable/refs/heads/main/loading.gif")
                            .error(Glide.with(DetailsActivity.this).asGif().load(R.raw.loading))
                            .into(episodesLoadingGif);
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    private void loadEpisodes(final int season) {
        if (episodeAdapter == null) return;
        showEpisodesLoading(true);
        episodeAdapter.setItems(new ArrayList<Episode>());
        
        if (isAnime && animeSeasonsMapping != null && !"Movie".equals(item.getType())) {
            int targetIndex = season - 1;
            if (targetIndex >= 0 && targetIndex < animeSeasonsMapping.length()) {
                String targetMalId = animeSeasonsMapping.optJSONObject(targetIndex).optString("mal_id");
                if (targetMalId != null && !targetMalId.isEmpty() && !"null".equals(targetMalId)) {
                    fetchJikanEpisodes(targetMalId);
                    return;
                }
            }
        }

        if (("Anime".equals(item.getType()) || "jikan".equals(item.getSource()) || isAnime) && !"Movie".equals(item.getType())) {
            String malId = item.getId();
            if (malId != null && !malId.isEmpty() && !"null".equals(malId)) {
                fetchJikanEpisodes(malId);
                return;
            }
        }
        
        if (imdbId != null && !imdbId.isEmpty()) {
            String posterUrl = (details != null && details.getPosterPath() != null) ? details.getPosterPath() : item.getPosterPath();
            ApiRepository.get().fetchEpisodesByImdb(imdbId, season, posterUrl, new ApiRepository.Callback<List<Episode>>() {
                public void onResult(List<Episode> result) {
                    showEpisodesLoading(false);
                    episodeAdapter.setItems(result);
                }
                public void onError(Exception e) {
                    showEpisodesLoading(false);
                }
            });
        } else {
            String fetchId = (details != null && details.getId() != null) ? details.getId() : item.getId();
            ApiRepository.get().fetchEpisodes(fetchId, season, new ApiRepository.Callback<List<Episode>>() {
                public void onResult(List<Episode> episodes) {
                    showEpisodesLoading(false);
                    episodeAdapter.setItems(episodes);
                }
                public void onError(Exception e) {
                    showEpisodesLoading(false);
                }
            });
        }
    }

    private void startPlaying(int season, int episode) {
        prefs.addToHistory(item);
        Intent i = new Intent(this, PlayerActivity.class);
        i.putExtra(Constants.EXTRA_ITEM, item);
        i.putExtra(Constants.EXTRA_SEASON, season);
        i.putExtra(Constants.EXTRA_EPISODE, episode);
        if (imdbId != null) i.putExtra("imdb_id", imdbId);
        if (details != null && details.getId() != null) i.putExtra("tmdb_id", details.getId());
        if (details != null && details.getAvailableSeasons() != null) {
            i.putIntegerArrayListExtra("available_seasons", new java.util.ArrayList<Integer>(details.getAvailableSeasons()));
        }
        startActivity(i);
    }
    
    private void fetchAnimeMapping() {
        if (imdbId == null || imdbId.isEmpty()) return;
        new Thread(() -> {
            try {
                if (animeSeasonsMapping == null) {
                    java.net.URL url = new java.net.URL(Constants.IMDB_MAL_BASE + "/?key=" + Constants.VAL_KEY + "&imdb=" + imdbId);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    java.io.InputStream is = conn.getInputStream();
                    java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                    int nRead;
                    byte[] data = new byte[1024];
                    while ((nRead = is.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    buffer.flush();
                    String json = new String(buffer.toByteArray(), "UTF-8");
                    org.json.JSONObject resultObj = new org.json.JSONObject(json);
                    if (resultObj.has("results")) {
                        org.json.JSONArray raw = resultObj.optJSONArray("results");
                        if (raw != null) {
                            animeSeasonsMapping = new org.json.JSONArray();
                            for (int i = 0; i < raw.length(); i++) {
                                org.json.JSONObject ro = raw.getJSONObject(i);
                                String mId = ro.optString("mal_id");
                                if (mId != null && !mId.isEmpty() && !mId.equals("null") && !mId.equals("0")) {
                                    animeSeasonsMapping.put(ro);
                                }
                            }
                        }
                    }
                }
                if (animeSeasonsMapping != null && animeSeasonsMapping.length() > 0) {
                    isAnime = true;
                    runOnUiThread(() -> {
                        List<Integer> s = new java.util.ArrayList<>();
                        List<String> names = new java.util.ArrayList<>();
                        for (int i = 0; i < animeSeasonsMapping.length(); i++) {
                            s.add(i + 1);
                            String apId = animeSeasonsMapping.optJSONObject(i).optString("anime-planet_id");
                            names.add(formatAnimeTitle(apId));
                        }
                        setupSeasons(s, names);
                    });
                }
            } catch (Exception e) {
            } finally {
                populateExclusiveStats(details != null ? details : null);
            }
        }).start();
    }

    private void fetchJikanEpisodes(String malId) {
        new Thread(() -> {
            try {
                // First fetch anime details to check num_episodes
                int metaNumEpisodes = 0;
                try {
                    java.net.URL metaUrl = new java.net.URL(Constants.JIKAN_BASE + "/anime/" + malId + "?key=" + Constants.VAL_KEY);
                    java.net.HttpURLConnection metaConn = (java.net.HttpURLConnection) metaUrl.openConnection();
                    metaConn.setRequestMethod("GET");
                    metaConn.setConnectTimeout(8000);
                    metaConn.setReadTimeout(8000);
                    if (metaConn.getResponseCode() == 200) {
                        java.io.InputStream is = metaConn.getInputStream();
                        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                        int nRead;
                        byte[] data = new byte[1024];
                        while ((nRead = is.read(data, 0, data.length)) != -1) {
                            buffer.write(data, 0, nRead);
                        }
                        buffer.flush();
                        String metaJson = new String(buffer.toByteArray(), "UTF-8");
                        org.json.JSONObject metaObj = new org.json.JSONObject(metaJson);
                        metaNumEpisodes = metaObj.optInt("num_episodes", metaObj.optInt("episodes_count", 0));
                        if (metaNumEpisodes <= 0) {
                            org.json.JSONObject dataObj = metaObj.optJSONObject("data");
                            if (dataObj != null) {
                                metaNumEpisodes = dataObj.optInt("episodes", dataObj.optInt("num_episodes", 0));
                            }
                        }
                    }
                } catch (Exception ignored) {}

                String fallbackCover = "";
                if (item != null) {
                    if (item.getPosterPath() != null && !item.getPosterPath().isEmpty() && !"null".equals(item.getPosterPath())) {
                        fallbackCover = item.getPosterPath();
                    } else if (item.getBackdropPath() != null && !item.getBackdropPath().isEmpty() && !"null".equals(item.getBackdropPath())) {
                        fallbackCover = item.getBackdropPath();
                    }
                }

                List<Episode> allEpisodes = new java.util.ArrayList<>();
                boolean hasNextPage = true;
                int page = 1;
                while (hasNextPage) {
                    String epUrlStr = Constants.JIKAN_BASE + "/anime/" + malId + "/episodes?key=" + Constants.VAL_KEY + (page > 1 ? "&page=" + page : "");
                    java.net.URL url = new java.net.URL(epUrlStr);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    java.io.InputStream is = conn.getInputStream();
                    java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                    int nRead;
                    byte[] data = new byte[1024];
                    while ((nRead = is.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    buffer.flush();
                    String json = new String(buffer.toByteArray(), "UTF-8");
                    org.json.JSONObject obj = new org.json.JSONObject(json);
                    int epCountInObj = obj.optInt("episodes_count", 0);
                    if (epCountInObj > metaNumEpisodes) metaNumEpisodes = epCountInObj;

                    org.json.JSONArray dataArr = obj.optJSONArray("episodes");
                    if (dataArr == null) {
                        dataArr = obj.optJSONArray("data");
                    }
                    
                    if (dataArr != null) {
                        for (int i = 0; i < dataArr.length(); i++) {
                            org.json.JSONObject epObj = dataArr.getJSONObject(i);
                            Episode ep = new Episode();
                            int epNum = epObj.optInt("episode", epObj.optInt("mal_id", i + 1));
                            ep.setEpisodeNumber(epNum);
                            String epTitle = epObj.optString("title", "");
                            if (epTitle.isEmpty() || "null".equals(epTitle)) {
                                epTitle = epObj.optString("title_japanese", "Episode " + epNum);
                            }
                            ep.setName(epTitle);
                            ep.setAirDate(epObj.optString("aired", ""));
                            String img = epObj.optString("image_url", "");
                            if (img != null && !img.isEmpty() && !img.equals("null")) {
                                ep.setStillPath(img);
                            } else if (!fallbackCover.isEmpty()) {
                                ep.setStillPath(fallbackCover);
                            }
                            ep.setSeasonNumber(currentSeason);
                            allEpisodes.add(ep);
                        }
                    }
                    org.json.JSONObject pagination = obj.optJSONObject("pagination");
                    hasNextPage = pagination != null && pagination.optBoolean("has_next_page", false);
                    page++;
                }

                // If num_episodes from /anime/{malId} is higher than /episodes count, expand list to the higher count
                int targetCount = Math.max(allEpisodes.size(), metaNumEpisodes);
                if (targetCount > allEpisodes.size()) {
                    java.util.HashSet<Integer> existingNums = new java.util.HashSet<>();
                    for (Episode existingEp : allEpisodes) {
                        existingNums.add(existingEp.getEpisodeNumber());
                    }
                    for (int epNum = 1; epNum <= targetCount; epNum++) {
                        if (!existingNums.contains(epNum)) {
                            Episode ep = new Episode();
                            ep.setEpisodeNumber(epNum);
                            ep.setName("Episode " + epNum);
                            ep.setAirDate("");
                            if (!fallbackCover.isEmpty()) {
                                ep.setStillPath(fallbackCover);
                            }
                            ep.setSeasonNumber(currentSeason);
                            allEpisodes.add(ep);
                        }
                    }
                    java.util.Collections.sort(allEpisodes, (a, b) -> Integer.compare(a.getEpisodeNumber(), b.getEpisodeNumber()));
                }

                runOnUiThread(() -> {
                    showEpisodesLoading(false);
                    if (episodeAdapter != null) episodeAdapter.setItems(allEpisodes);
                });
            } catch (Exception e) {
                showEpisodesLoading(false);
            }
        }).start();
    }

    private void openDetails(MediaItem other) {
        Intent i = new Intent(this, DetailsActivity.class);
        i.putExtra(Constants.EXTRA_ITEM, other);
        startActivity(i);
    }

    private void updateBookmarkIcon() {
        boolean saved = prefs.isInWatchlist(item);
        bookmarkBtn.setImageResource(saved ? R.drawable.ic_bookmark_fill : R.drawable.ic_bookmark);
        bookmarkBtn.setColorFilter(saved ? 0xFF00FFFF : 0xFFFFFFFF);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resolveAnimeMappingAndStats();
    }
}

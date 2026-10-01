package com.watchable;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.watchable.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenreActivity extends AppCompatActivity {

    private static final Map<String, int[]> GENRE_IDS = new HashMap<String, int[]>();
    static {
        GENRE_IDS.put("Action",    new int[]{28,    10759, 1});
        GENRE_IDS.put("Adventure", new int[]{12,    10759, 2});
        GENRE_IDS.put("Comedy",    new int[]{35,    35,    4});
        GENRE_IDS.put("Drama",     new int[]{18,    18,    8});
        GENRE_IDS.put("Fantasy",   new int[]{14,    10765, 10});
        GENRE_IDS.put("Horror",    new int[]{27,    27,    14});
        GENRE_IDS.put("Mystery",   new int[]{9648,  9648,  7});
        GENRE_IDS.put("Romance",   new int[]{10749, 10749, 22});
        GENRE_IDS.put("Sci-Fi",    new int[]{878,   10765, 24});
        GENRE_IDS.put("Thriller",  new int[]{53,    53,    41});
    }

    private String currentGenre;
    private String currentType = "All";
    private int currentPage = 1;
    private boolean isLoading = false;
    private GridAdapter adapter;
    private RecyclerView recyclerView;
    private ProgressBar spinner;
    private LinearLayout genreChipsContainer, typeTabsContainer;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_genre);

        currentGenre = getIntent().getStringExtra(Constants.EXTRA_GENRE);
        if (currentGenre == null) currentGenre = "Action";

        genreChipsContainer = findViewById(R.id.genre_chips_row);
        typeTabsContainer   = findViewById(R.id.genre_type_tabs);
        recyclerView        = findViewById(R.id.genre_recycler);
        spinner             = findViewById(R.id.genre_spinner);

        findViewById(R.id.genre_back_btn).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        adapter = new GridAdapter(new GridAdapter.OnItemClick() {
            public void onClick(MediaItem item) {
                Intent i = new Intent(GenreActivity.this, DetailsActivity.class);
                i.putExtra(Constants.EXTRA_ITEM, item);
                startActivity(i);
            }
        });
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                GridLayoutManager lm = (GridLayoutManager) rv.getLayoutManager();
                if (!isLoading && lm != null
                        && lm.findLastVisibleItemPosition() >= lm.getItemCount() - 6) {
                    currentPage++;
                    loadPage(false);
                }
            }
        });

        buildGenreChips();
        buildTypeTabs();
        loadPage(true);
    }

    private void buildGenreChips() {
        genreChipsContainer.removeAllViews();
        for (final String genre : Constants.GENRES) {
            TextView chip = new TextView(this);
            chip.setText(genre);
            chip.setTextSize(11f);
            chip.setPadding(dp(16), dp(6), dp(16), dp(6));
            chip.setBackgroundResource(genre.equals(currentGenre)
                ? R.drawable.bg_genre_chip_active : R.drawable.bg_genre_chip);
            chip.setTextColor(genre.equals(currentGenre) ? Color.BLACK : 0xFF00FFFF);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            chip.setLayoutParams(lp);
            chip.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    currentGenre = genre; currentPage = 1;
                    adapter.setItems(new ArrayList<MediaItem>());
                    buildGenreChips(); loadPage(true);
                }
            });
            genreChipsContainer.addView(chip);
        }
    }

    private void buildTypeTabs() {
        typeTabsContainer.removeAllViews();
        final String[] types = {"All", "Movie", "TV Show", "Anime"};
        for (final String type : types) {
            TextView tab = new TextView(this);
            tab.setText(type);
            tab.setTextSize(10f);
            tab.setPadding(dp(16), dp(8), dp(16), dp(8));
            tab.setAllCaps(true);
            boolean active = type.equals(currentType);
            tab.setTextColor(active ? 0xFF00FFFF : 0xFF71717A);
            if (active) tab.setBackgroundResource(R.drawable.bg_type_tab_active);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            tab.setLayoutParams(lp);
            tab.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    currentType = type; currentPage = 1;
                    adapter.setItems(new ArrayList<MediaItem>());
                    buildTypeTabs(); loadPage(true);
                }
            });
            typeTabsContainer.addView(tab);
        }
    }

    private void loadPage(boolean reset) {
        if (isLoading) return;
        isLoading = true;
        spinner.setVisibility(View.VISIBLE);
        if (reset) { adapter.setItems(new ArrayList<MediaItem>()); currentPage = 1; }

        int[] ids = GENRE_IDS.get(currentGenre);
        if (ids == null) { isLoading = false; spinner.setVisibility(View.GONE); return; }

        final int[] pending = {0};
        final ApiRepository.Callback<List<MediaItem>> merge = new ApiRepository.Callback<List<MediaItem>>() {
            public void onResult(final List<MediaItem> r) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        pending[0]--;
                        if (r != null && !r.isEmpty()) {
                            adapter.addItems(r);
                            spinner.setVisibility(View.GONE);
                        }
                        if (pending[0] <= 0) {
                            isLoading = false;
                            spinner.setVisibility(View.GONE);
                        }
                    }
                });
            }
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        pending[0]--;
                        if (pending[0] <= 0) {
                            isLoading = false;
                            spinner.setVisibility(View.GONE);
                        }
                    }
                });
            }
        };

        if ("All".equals(currentType) || "Movie".equals(currentType))   { pending[0]++; ApiRepository.get().fetchMoviesByGenre(ids[0], currentPage, merge); }
        if ("All".equals(currentType) || "TV Show".equals(currentType)) { pending[0]++; ApiRepository.get().fetchTvByGenre(ids[1], currentPage, merge); }
        if ("All".equals(currentType) || "Anime".equals(currentType))   { pending[0]++; ApiRepository.get().fetchAnimeByGenre(ids[2], currentPage, merge); }
        if (pending[0] == 0) { isLoading = false; spinner.setVisibility(View.GONE); }
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
}

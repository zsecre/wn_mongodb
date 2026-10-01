package com.watchable;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.watchable.R;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {
    private EditText searchInput;
    private ProgressBar spinner;
    private RecyclerView recycler;
    private TextView emptyText;
    private GridAdapter adapter;
    private final Handler handler = new Handler();
    private Runnable searchRunnable;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        searchInput = findViewById(R.id.search_input);
        spinner     = findViewById(R.id.search_spinner);
        recycler    = findViewById(R.id.search_recycler);
        emptyText   = findViewById(R.id.search_empty);

        findViewById(R.id.search_back_btn).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        adapter = new GridAdapter(new GridAdapter.OnItemClick() {
            public void onClick(MediaItem item) {
                Intent i = new Intent(SearchActivity.this, DetailsActivity.class);
                i.putExtra(Constants.EXTRA_ITEM, item);
                startActivity(i);
            }
        });
        recycler.setLayoutManager(new GridLayoutManager(this, 3));
        recycler.setAdapter(adapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void afterTextChanged(Editable s) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                if (searchRunnable != null) handler.removeCallbacks(searchRunnable);
                final String query = s.toString().trim();
                if (query.length() < 2) {
                    spinner.setVisibility(View.GONE);
                    recycler.setVisibility(View.GONE);
                    emptyText.setVisibility(View.VISIBLE);
                    emptyText.setText("Type to search movies, TV shows & anime");
                    adapter.setItems(new ArrayList<MediaItem>());
                    return;
                }
                spinner.setVisibility(View.VISIBLE);
                searchRunnable = new Runnable() {
                    public void run() { doSearch(query); }
                };
                handler.postDelayed(searchRunnable, 300);
            }
        });
        searchInput.requestFocus();
    }

    private void doSearch(String query) {
        ApiRepository.get().search(query, new ApiRepository.Callback<List<MediaItem>>() {
            public void onResult(final List<MediaItem> items) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (items != null && !items.isEmpty()) {
                            spinner.setVisibility(View.GONE);
                            emptyText.setVisibility(View.GONE);
                            recycler.setVisibility(View.VISIBLE);
                            adapter.setItems(items);
                        } else if (adapter.getItemCount() == 0) {
                            spinner.setVisibility(View.GONE);
                            recycler.setVisibility(View.GONE);
                            emptyText.setVisibility(View.VISIBLE);
                            emptyText.setText("No results found");
                        } else {
                            spinner.setVisibility(View.GONE);
                        }
                    }
                });
            }
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (adapter.getItemCount() == 0) {
                            spinner.setVisibility(View.GONE);
                            emptyText.setVisibility(View.VISIBLE);
                            emptyText.setText("Search failed. Try again.");
                        } else {
                            spinner.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });
    }
}

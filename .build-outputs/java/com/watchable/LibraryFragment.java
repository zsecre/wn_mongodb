package com.watchable;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.watchable.R;
import java.util.List;

public class LibraryFragment extends BaseTabFragment {
    private View root;

    @Nullable public View onCreateView(@NonNull LayoutInflater inf, ViewGroup c, Bundle s) {
        root = inf.inflate(R.layout.fragment_library, c, false);
        setupSearchBtn(root);
        return root;
    }

    public void onResume() {
        super.onResume();
        setupContent();
    }

    private void setupContent() {
        if (!isAdded() || root == null) return;
        AppPreferences prefs = AppPreferences.get(requireContext());
        final List<MediaItem> watchlist = prefs.getWatchlist();
        final List<MediaItem> history   = prefs.getHistory();

        LinearLayout emptyCollection = root.findViewById(R.id.empty_collection);
        LinearLayout emptyHistory    = root.findViewById(R.id.empty_history);
        View rowCollection = root.findViewById(R.id.row_collection);
        View rowHistory    = root.findViewById(R.id.row_history);

        if (!watchlist.isEmpty()) {
            emptyCollection.setVisibility(View.GONE);
            rowCollection.setVisibility(View.VISIBLE);
            RecyclerView rv = rowCollection.findViewById(R.id.row_recycler);
            rv.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            rv.setAdapter(new MediaRowAdapter(watchlist, new MediaRowAdapter.OnItemClick() {
                public void onClick(MediaItem item) {
                    LibraryFragment.this.openDetails(item);
                }
            }));
            rowCollection.findViewById(R.id.row_view_all).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    LibraryFragment.this.openViewAll("Collection", watchlist);
                }
            });
            ((TextView) rowCollection.findViewById(R.id.row_title)).setText("Collection");
        } else {
            emptyCollection.setVisibility(View.VISIBLE);
            rowCollection.setVisibility(View.GONE);
        }

        if (!history.isEmpty()) {
            emptyHistory.setVisibility(View.GONE);
            rowHistory.setVisibility(View.VISIBLE);
            RecyclerView rv = rowHistory.findViewById(R.id.row_recycler);
            rv.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            rv.setAdapter(new MediaRowAdapter(history, new MediaRowAdapter.OnItemClick() {
                public void onClick(MediaItem item) {
                    LibraryFragment.this.openDetails(item);
                }
            }));
            rowHistory.findViewById(R.id.row_view_all).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    LibraryFragment.this.openViewAll("Watch History", history);
                }
            });
            ((TextView) rowHistory.findViewById(R.id.row_title)).setText("Watch History");
        } else {
            emptyHistory.setVisibility(View.VISIBLE);
            rowHistory.setVisibility(View.GONE);
        }
    }
}

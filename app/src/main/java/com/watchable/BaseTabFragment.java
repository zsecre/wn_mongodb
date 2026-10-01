package com.watchable;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.watchable.R;
import me.relex.circleindicator.CircleIndicator3;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public abstract class BaseTabFragment extends Fragment {
    protected Timer slideshowTimer;
    protected View notifBadgeView;

    @Override
    public void onResume() {
        super.onResume();
        refreshNotificationBadge();
    }
    
    private void refreshNotificationBadge() {
        if (notifBadgeView == null || !isAdded()) return;
        String userId = AppPreferences.get(requireContext()).getUserId();
        if (userId == null) {
            android.content.SharedPreferences prefs = requireContext().getSharedPreferences("video_stats", Context.MODE_PRIVATE);
            userId = prefs.getString("user_id_uuid", null);
        }
        if (userId != null) {
            DatabaseService.get().getUnreadNotificationCount(userId, new DatabaseService.DataCallback<Integer>() {
                @Override
                public void onSuccess(Integer unread) {
                    if (getActivity() != null && notifBadgeView != null) {
                        notifBadgeView.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
                    }
                }

                @Override
                public void onError(Exception e) {}
            });
        }
    }

    protected void setupSearchBtn(View root) {
        View.OnClickListener searchListener = new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(requireContext(), SearchActivity.class));
            }
        };

        View btn = root.findViewById(R.id.header_search_btn);
        if (btn != null) {
            btn.setOnClickListener(searchListener);
        }
        View footer = root.findViewById(R.id.layout_search_footer);
        if (footer != null) {
            footer.setOnClickListener(searchListener);
        }
        View footerBtn = root.findViewById(R.id.btn_footer_search);
        if (footerBtn != null) {
            footerBtn.setOnClickListener(searchListener);
        }
        
        View notifBtn = root.findViewById(R.id.header_notification_btn);
        notifBadgeView = root.findViewById(R.id.header_notification_badge);
        if (notifBtn != null) {
            notifBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    NotificationsBottomSheet sheet = NotificationsBottomSheet.newInstance();
                    sheet.setCountListener(new NotificationsBottomSheet.CountListener() {
                        public void onCountReady(int unreadCount) {
                            if (notifBadgeView != null) {
                                notifBadgeView.setVisibility(unreadCount > 0 ? View.VISIBLE : View.GONE);
                            }
                        }
                    });
                    sheet.show(getChildFragmentManager(), "NotificationsBottomSheet");
                }
            });
            refreshNotificationBadge();
        }
    }

    protected void setupSlideshow(View root, final List<MediaItem> items) {
        final ViewPager2 pager = root.findViewById(R.id.slideshow_pager);
        CircleIndicator3 indicator = root.findViewById(R.id.slideshow_indicator);
        if (pager == null || items == null || items.isEmpty()) return;
        final List<MediaItem> show = new ArrayList<MediaItem>(items.subList(0, Math.min(12, items.size())));
        
        if (pager.getAdapter() == null) {
            pager.setAdapter(new SlideAdapter(show, new SlideAdapter.OnClick() {
                public void onClick(MediaItem item) { openDetails(item); }
            }));
            if (indicator != null) indicator.setViewPager(pager);
            startAutoSlide(pager, show.size());
        } else {
            SlideAdapter adapter = (SlideAdapter) pager.getAdapter();
            adapter.setItems(show);
            adapter.notifyDataSetChanged();
            if (indicator != null) indicator.setViewPager(pager);
            startAutoSlide(pager, show.size());
        }
    }

    protected void startAutoSlide(final ViewPager2 pager, final int count) {
        if (slideshowTimer != null) { slideshowTimer.cancel(); slideshowTimer = null; }
        slideshowTimer = new Timer();
        slideshowTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (pager == null || !isAdded() || getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    public void run() { 
                        if (pager.getAdapter() != null && pager.getAdapter().getItemCount() > 0) {
                            pager.setCurrentItem((pager.getCurrentItem()+1)%count, true); 
                        }
                    }
                });
                refreshNotificationBadge();
            }
        }, 5000, 5000);
    }

    protected void setupMediaRow(View root, int rowId, final String title,
                                  List<MediaItem> items, final List<MediaItem> allItems, final String allTitle) {
        View row = root.findViewById(rowId);
        if (row == null || items == null || items.isEmpty()) {
            if (row != null) row.setVisibility(View.GONE); return;
        }
        row.setVisibility(View.VISIBLE);
        TextView tv = row.findViewById(R.id.row_title);
        if (tv != null) tv.setText(title);
        TextView viewAll = row.findViewById(R.id.row_view_all);
        RecyclerView rv = row.findViewById(R.id.row_recycler);
        rv.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rv.setAdapter(new MediaRowAdapter(items, new MediaRowAdapter.OnItemClick() {
            public void onClick(MediaItem item) { openDetails(item); }
        }));
        if (viewAll != null) {
            viewAll.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    openViewAll(allTitle, allItems != null ? allItems : new ArrayList<MediaItem>());
                }
            });
        }
    }

    protected void setupGenreChips(View root) {
        LinearLayout container = root.findViewById(R.id.genre_chips_container);
        if (container == null) return;
        container.removeAllViews();
        Context ctx = requireContext();
        for (final String genre : Constants.GENRES) {
            TextView chip = new TextView(ctx);
            chip.setText(genre);
            chip.setTextColor(0xFF00FFFF);
            chip.setTextSize(12f);
            chip.setPadding(dp(16), dp(6), dp(16), dp(6));
            chip.setBackgroundResource(R.drawable.bg_genre_chip);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            chip.setLayoutParams(lp);
            chip.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { openGenre(genre); }
            });
            container.addView(chip);
        }
    }

    protected void openDetails(MediaItem item) {
        Intent i = new Intent(requireContext(), DetailsActivity.class);
        i.putExtra(Constants.EXTRA_ITEM, item);
        startActivity(i);
    }

    protected void openViewAll(String title, List<MediaItem> items) {
        Intent i = new Intent(requireContext(), ViewAllActivity.class);
        i.putExtra(Constants.EXTRA_TITLE, title);
        i.putExtra(Constants.EXTRA_ITEMS, new ArrayList<MediaItem>(items));
        startActivity(i);
    }

    protected void openGenre(String genre) {
        Intent i = new Intent(requireContext(), GenreActivity.class);
        i.putExtra(Constants.EXTRA_GENRE, genre);
        startActivity(i);
    }

    protected int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density);
    }

    public void onDestroyView() {
        super.onDestroyView();
        if (slideshowTimer != null) { slideshowTimer.cancel(); slideshowTimer = null; }
    }
}

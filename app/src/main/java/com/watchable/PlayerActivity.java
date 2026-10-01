package com.watchable;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.watchable.R;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.json.JSONObject;
import java.io.IOException;

public class PlayerActivity extends AppCompatActivity {
    private WebView playerWebView;
    private MediaItem item;
    private String imdbId, tmdbId;
    private int currentSeason, currentEpisode;
    private final Handler handler = new Handler();
    private EpisodeAdapter episodeAdapter;
    private TextView nowPlayingTitle, nowPlayingSub;
    private NativeAd currentNativeAd;
    private AdView topAdView;
    private int selectedServer = 0;
    private String currentMalId = null;
    private int animeMode = 0;
    private org.json.JSONArray animeSeasonsMapping = null;
    private View customVideoView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private FrameLayout customVideoContainer;
    private View mainPlayerContent;
    private View adLoadingOverlay;
    private ImageView adLoadingGif;
    private View playerEpisodesLoadingContainer;
    private ImageView playerEpisodesLoadingGif;
    private TextView playerEpisodesLoadingText;
    private android.widget.EditText playerEpisodeInput;
    private Button playerEpisodeGo;
    private LinearLayout playerSeasonsRow;
    private View playerSeasonsScroll;

    private static long lastInterstitialShownTime = 0;
    private static final long INTERSTITIAL_COOLDOWN_MS = 20 * 1000; // 20 seconds cooldown
    private static final long NATIVE_AD_REFRESH_INTERVAL_MS = (1 * 60 + 40) * 1000; // 1 minute and 40 seconds (100 seconds)
    private final Handler nativeAdRefreshHandler = new Handler(android.os.Looper.getMainLooper());
    private final Runnable nativeAdRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isFinishing() && !isDestroyed()) {
                loadNativeAd();
            }
        }
    };

    private TextView statViews, statRating, statLikes, statDislikes, statImdbRating;
    private com.google.android.material.button.MaterialButton actionLike, actionDislike, actionRateBtn;
    private String currentDbKey = "";
    private OkHttpClient httpClient = new OkHttpClient();
    private boolean viewRecorded = false;
    private android.content.SharedPreferences prefs;
    private Handler statsHandler = new Handler(android.os.Looper.getMainLooper());
    private Runnable viewRunnable = new Runnable() {
        @Override
        public void run() {
            if (!viewRecorded && !currentDbKey.isEmpty()) {
                viewRecorded = true;
                recordStat("views", 1, 0);
            }
        }
    };
    private final String DB_URL = Constants.DB_URL;

    private boolean isAllowedPlayerDomain(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        String[] allowed = {
            "cinesrc", "vidfast", "vidsrc", "vsembed", "vidrock",
            "moviesapi", "autoembed", "embed.su", "megaplay", "superembed",
            "2embed", "smashystream", "tmdb.org", "themoviedb.org", "imdb.com",
            "google.com/recaptcha", "gstatic.com", "cloudflare.com", "challenges.cloudflare.com",
            "googleapis.com", "cdnjs.cloudflare.com", "jsdelivr.net", "m3u8", ".m3u8",
            "rabbitstream", "megacloud", "dokicloud", "streamwish", "filelions", "dropload",
            "vidsrc.me", "vidsrc.to", "vidsrc.cc", "vidsrc.pm", "vidsrc.in", "vidsrc.net", "vidsrc.xyz"
        };
        for (String a : allowed) {
            if (lower.contains(a)) return true;
        }
        return false;
    }

    private boolean isAdUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return true;
        }
        if (isAllowedPlayerDomain(url)) {
            return false;
        }
        String[] adDomains = {
            "doubleclick.net", "googleads.", "adservice.google", "adserver",
            "propellerads", "histats.com", "exoclick.com", "juicyads.com",
            "adsterra.com", "bet365", "1xbet", "revenuehits.com", "yllix.com",
            "adcash.com", "hilltopads.com", "monetag.com", "highperformancecpm.com",
            "deloton.com", "alwingulla.com", "coinhive", "/popunder", "/adclick",
            "onclickads", "syndication.exdynsrv", "popads.net", "trafficfactory.biz",
            "adnxs.com", "popcash.net", "adtrue.com", "clickadu.com", "ad-delivery.net"
        };
        for (String p : adDomains) {
            if (lower.contains(p)) {
                return true;
            }
        }
        return false;
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("video_stats", MODE_PRIVATE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        setContentView(R.layout.activity_player);
        
        customVideoContainer = findViewById(R.id.player_custom_video_container);
        mainPlayerContent = findViewById(R.id.player_main_content);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (customVideoView != null) {
                    if (playerWebView != null && playerWebView.getWebChromeClient() != null) {
                        WebChromeClient client = (WebChromeClient) playerWebView.getWebChromeClient();
                        // Exit fullscreen custom view
                        if (customViewCallback != null) {
                            customViewCallback.onCustomViewHidden();
                            customViewCallback = null;
                        }
                    }
                    if (customVideoContainer != null) {
                        customVideoContainer.removeAllViews();
                        customVideoContainer.setVisibility(View.GONE);
                    }
                    if (mainPlayerContent != null) {
                        mainPlayerContent.setVisibility(View.VISIBLE);
                    }
                    customVideoView = null;
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    return;
                }
                if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    return;
                }
                // Directly finish the player screen
                finish();
            }
        });
        
        item           = (MediaItem) getIntent().getSerializableExtra(Constants.EXTRA_ITEM);
        imdbId         = getIntent().getStringExtra("imdb_id");
        tmdbId         = getIntent().getStringExtra("tmdb_id");
        currentSeason  = getIntent().getIntExtra(Constants.EXTRA_SEASON, 1);
        currentEpisode = getIntent().getIntExtra(Constants.EXTRA_EPISODE, 1);
        if (item == null) { finish(); return; }

        playerWebView   = findViewById(R.id.player_webview);
        nowPlayingTitle = findViewById(R.id.player_title);
        nowPlayingSub   = findViewById(R.id.player_subtitle);
        playerEpisodeInput = findViewById(R.id.player_episode_input);
        playerEpisodeGo = findViewById(R.id.player_episode_go);
        playerSeasonsRow = findViewById(R.id.player_seasons_row);
        playerSeasonsScroll = findViewById(R.id.player_seasons_scroll);
        adLoadingOverlay = findViewById(R.id.player_ad_loading_overlay);
        adLoadingGif = findViewById(R.id.player_ad_loading_gif);
        playerEpisodesLoadingContainer = findViewById(R.id.player_episodes_loading_container);
        playerEpisodesLoadingGif       = findViewById(R.id.player_episodes_loading_gif);
        playerEpisodesLoadingText      = findViewById(R.id.player_episodes_loading_text);

        WebSettings ws = playerWebView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setDomStorageEnabled(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        ws.setCacheMode(WebSettings.LOAD_DEFAULT);
        ws.setUserAgentString("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36");
        
        android.webkit.CookieManager cookieManager = android.webkit.CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(playerWebView, true);
        
        ws.setSupportMultipleWindows(false);
        ws.setJavaScriptCanOpenWindowsAutomatically(false);
        ws.setDatabaseEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(true);

        playerWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (isAdUrl(url)) {
                    return true;
                }
                // Allow legitimate media player domains and navigations
                if (request.isForMainFrame()) {
                    if (!isAllowedPlayerDomain(url)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (isAdUrl(url)) {
                    return true;
                }
                if (!isAllowedPlayerDomain(url)) {
                    return true;
                }
                return false;
            }

            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (isAdUrl(url)) {
                    return new android.webkit.WebResourceResponse("text/plain", "utf-8", new java.io.ByteArrayInputStream("".getBytes()));
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                
                // Neutralize window.open and popup hijackers without breaking player controls
                String antiPopupJs = 
                    "(function() {" +
                    "  try {" +
                    "    window.open = function() { return null; };" +
                    "    window.alert = function() {};" +
                    "    window.confirm = function() { return true; };" +
                    "    window.prompt = function() { return null; };" +
                    "  } catch(e) {}" +
                    "})();";
                view.evaluateJavascript(antiPopupJs, null);

                // Remove chevron button / episode drawer toggle in upper right of cinesrc TV player & compact settings menu
                String playerEnhanceJs = 
                    "(function() {" +
                    "  try {" +
                    "    function applyCompactSettings(doc) {" +
                    "      if (!doc) return;" +
                    "      var styleId = 'watchable-compact-settings-menu';" +
                    "      if (!doc.getElementById(styleId)) {" +
                    "        var style = doc.createElement('style');" +
                    "        style.id = styleId;" +
                    "        style.innerHTML = '" +
                    "          .chevron, [class*=\"chevron\" i], [class*=\"Chevron\" i], [id*=\"chevron\" i]," +
                    "          [class*=\"episode-select\" i], [class*=\"episodes-select\" i], [class*=\"episode_select\" i]," +
                    "          [class*=\"episodes-button\" i], [class*=\"episode-button\" i], [class*=\"episodes-btn\" i]," +
                    "          [class*=\"season-select\" i], [class*=\"seasons-select\" i]," +
                    "          .vjs-episodes-button, .jw-icon-episodes, .art-episodes-button," +
                    "          button[aria-label*=\"Episode\" i], button[aria-label*=\"Season\" i]," +
                    "          button[title*=\"Episode\" i], button[title*=\"Season\" i]," +
                    "          .cinesrc-episodes, .cinesrc-episodes-btn, .episodes-trigger," +
                    "          .server-episode-btn, .top-right-chevron," +
                    "          [class*=\"top-right\"] svg, [class*=\"top_right\"] svg," +
                    "          [class*=\"top-right\"] button, [class*=\"top_right\"] button {" +
                    "            display: none !important;" +
                    "            visibility: hidden !important;" +
                    "            opacity: 0 !important;" +
                    "            pointer-events: none !important;" +
                    "            max-width: 0 !important;" +
                    "            max-height: 0 !important;" +
                    "          }" +
                    "          .art-setting, .art-settings, .art-setting-panel, .art-setting-inner," +
                    "          .vjs-menu, .vjs-menu-content, .vjs-settings-menu," +
                    "          .jw-settings-menu, .jw-settings-submenu, .jw-settings-content," +
                    "          .plyr__menu__container, .vds-menu, .vds-menu-items," +
                    "          [class*=\"setting-panel\" i], [class*=\"settings-panel\" i]," +
                    "          [class*=\"settings-menu\" i], [class*=\"setting-menu\" i]," +
                    "          [class*=\"player-settings\" i], [class*=\"settings-dialog\" i]," +
                    "          [class*=\"settings-modal\" i], [class*=\"settings-drawer\" i]," +
                    "          [class*=\"settings-dropdown\" i] {" +
                    "            zoom: 0.82 !important;" +
                    "            max-height: 82% !important;" +
                    "          }" +
                    "          .art-setting-item, .vjs-menu-item, .jw-settings-item, .vds-menu-item," +
                    "          [class*=\"setting-item\" i], [class*=\"settings-item\" i], [class*=\"settings-option\" i] {" +
                    "            font-size: 12px !important;" +
                    "            padding-top: 4px !important;" +
                    "            padding-bottom: 4px !important;" +
                    "            min-height: 26px !important;" +
                    "            line-height: 1.25 !important;" +
                    "          }" +
                    "          .art-setting-header, .jw-settings-topbar," +
                    "          [class*=\"setting-header\" i], [class*=\"settings-header\" i] {" +
                    "            font-size: 13px !important;" +
                    "            padding: 6px 10px !important;" +
                    "          }';" +
                    "        if (doc.head) doc.head.appendChild(style);" +
                    "        else if (doc.documentElement) doc.documentElement.appendChild(style);" +
                    "      }" +
                    "    }" +
                    "    function applyAllDocs(rootDoc) {" +
                    "      if (!rootDoc) return;" +
                    "      applyCompactSettings(rootDoc);" +
                    "      try {" +
                    "        var iframes = rootDoc.querySelectorAll('iframe');" +
                    "        for (var i = 0; i < iframes.length; i++) {" +
                    "          try {" +
                    "            var cDoc = iframes[i].contentDocument || (iframes[i].contentWindow && iframes[i].contentWindow.document);" +
                    "            if (cDoc) applyCompactSettings(cDoc);" +
                    "          } catch(e) {}" +
                    "        }" +
                    "      } catch(e) {}" +
                    "    }" +
                    "    function hideElements(rootDoc) {" +
                    "      if (!rootDoc) return;" +
                    "      var w = window.innerWidth || document.documentElement.clientWidth;" +
                    "      var els = rootDoc.querySelectorAll('button, a, div, span, svg, i, path');" +
                    "      for (var i = 0; i < els.length; i++) {" +
                    "        var el = els[i];" +
                    "        var cls = (el.className && typeof el.className === \"string\") ? el.className.toLowerCase() : \"\";" +
                    "        var id = (el.id && typeof el.id === \"string\") ? el.id.toLowerCase() : \"\";" +
                    "        var aria = (el.getAttribute(\"aria-label\") || \"\").toLowerCase();" +
                    "        var title = (el.getAttribute(\"title\") || \"\").toLowerCase();" +
                    "        if (cls.indexOf(\"setting\") !== -1 || id.indexOf(\"setting\") !== -1 || aria.indexOf(\"setting\") !== -1 || title.indexOf(\"setting\") !== -1) continue;" +
                    "        if (el.closest && (el.closest('[class*=\"setting\" i]') || el.closest('[class*=\"menu\" i]'))) continue;" +
                    "        if (cls.indexOf(\"chevron\") !== -1 || id.indexOf(\"chevron\") !== -1 || " +
                    "            cls.indexOf(\"arrow-down\") !== -1 || cls.indexOf(\"arrow-right\") !== -1 || " +
                    "            cls.indexOf(\"angle-down\") !== -1 || cls.indexOf(\"episodes\") !== -1 || " +
                    "            id.indexOf(\"episodes\") !== -1 || aria.indexOf(\"episode\") !== -1 || " +
                    "            title.indexOf(\"episode\") !== -1) {" +
                    "          el.style.setProperty(\"display\", \"none\", \"important\");" +
                    "          el.style.setProperty(\"visibility\", \"hidden\", \"important\");" +
                    "        }" +
                    "        try {" +
                    "          var rect = el.getBoundingClientRect();" +
                    "          if (rect.top >= 0 && rect.top <= 75 && rect.right >= (w - 90) && rect.width > 0 && rect.width <= 70 && rect.height <= 70) {" +
                    "            if (el.tagName === \"BUTTON\" || el.tagName === \"A\" || el.tagName === \"SVG\" || cls.indexOf(\"btn\") !== -1 || cls.indexOf(\"icon\") !== -1 || el.querySelector(\"svg, i, path\")) {" +
                    "              el.style.setProperty(\"display\", \"none\", \"important\");" +
                    "              el.style.setProperty(\"visibility\", \"hidden\", \"important\");" +
                    "            }" +
                    "          }" +
                    "        } catch(e) {}" +
                    "      }" +
                    "    }" +
                    "    function runAll() {" +
                    "      applyAllDocs(document);" +
                    "      hideElements(document);" +
                    "    }" +
                    "    runAll();" +
                    "    var obs = new MutationObserver(runAll);" +
                    "    obs.observe(document.documentElement || document.body, { childList: true, subtree: true, attributes: true });" +
                    "    setInterval(runAll, 500);" +
                    "  } catch(e) {}" +
                    "})();";
                view.evaluateJavascript(playerEnhanceJs, null);
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                super.doUpdateVisitedHistory(view, url, isReload);
                checkUrlForEpisodeChange(url);
            }
        });

        playerWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customVideoView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customVideoView = view;
                customViewCallback = callback;
                
                if (mainPlayerContent != null) {
                    mainPlayerContent.setVisibility(View.GONE);
                }
                if (customVideoContainer != null) {
                    customVideoContainer.setVisibility(View.VISIBLE);
                    customVideoContainer.removeAllViews();
                    customVideoContainer.addView(view, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    ));
                }
                
                // Hide system UI (immersive sticky fullscreen)
                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }

            @Override
            public void onHideCustomView() {
                if (customVideoView == null) return;
                
                if (customVideoContainer != null) {
                    customVideoContainer.removeAllViews();
                    customVideoContainer.setVisibility(View.GONE);
                }
                if (mainPlayerContent != null) {
                    mainPlayerContent.setVisibility(View.VISIBLE);
                }
                
                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                    customViewCallback = null;
                }
                customVideoView = null;
                
                // Restore system UI
                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }

            @Override
            public android.graphics.Bitmap getDefaultVideoPoster() {
                return android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888);
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                // Return false to explicitly decline creation of popup windows/tabs from ad scripts
                return false;
            }
        });

        playWithAd(new Runnable() {
            @Override
            public void run() {
                loadVideo();
            }
        });
        fetchAnimeMapping();
        setupServers();
        setupEpisodes();
        updateNowPlaying();
        setupSimilarShows();

        statViews = findViewById(R.id.stat_views);
        statRating = findViewById(R.id.stat_rating);
        statImdbRating = findViewById(R.id.stat_imdb_rating);
        statLikes = findViewById(R.id.stat_likes);
        statDislikes = findViewById(R.id.stat_dislikes);
        
        if (item != null && statImdbRating != null) {
            statImdbRating.setText(item.getVoteAverage() > 0 ? String.format("IMDB Ratings: %.1f", item.getVoteAverage()) : "IMDB Ratings: N/A");
            loadImdbRating(imdbId);
        }
        actionRateBtn = findViewById(R.id.action_rate_btn);
        actionLike = findViewById(R.id.action_like);
        actionDislike = findViewById(R.id.action_dislike);
        if (actionLike != null) {
            actionLike.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { 
                    boolean isLiked = prefs.getBoolean(currentDbKey + "_liked", false);
                    boolean isDisliked = prefs.getBoolean(currentDbKey + "_disliked", false);
                    if (isLiked) {
                        recordStat("likes", -1, 0);
                        prefs.edit().putBoolean(currentDbKey + "_liked", false).apply();
                        actionLike.setIconResource(R.drawable.ic_thumb_up_outline);
                        try {
                            String txt = actionLike.getText().toString();
                            int cur = txt.isEmpty() ? 0 : Integer.parseInt(txt);
                            actionLike.setText(cur - 1 > 0 ? String.valueOf(cur - 1) : "");
                        } catch(Exception e) {}
                    } else {
                        recordStat("likes", 1, 0);
                        prefs.edit().putBoolean(currentDbKey + "_liked", true).apply();
                        actionLike.setIconResource(R.drawable.ic_thumb_up);
                        try {
                            String txt = actionLike.getText().toString();
                            int cur = txt.isEmpty() ? 0 : Integer.parseInt(txt);
                            actionLike.setText(String.valueOf(cur + 1));
                        } catch(Exception e) {}
                        
                        if (isDisliked) {
                            recordStat("dislikes", -1, 0);
                            prefs.edit().putBoolean(currentDbKey + "_disliked", false).apply();
                            actionDislike.setIconResource(R.drawable.ic_thumb_down_outline);
                            try {
                                String txt = actionDislike.getText().toString();
                                int cur = txt.isEmpty() ? 0 : Integer.parseInt(txt);
                                actionDislike.setText(cur - 1 > 0 ? String.valueOf(cur - 1) : "");
                            } catch(Exception e) {}
                        }
                    }
                }
            });
            actionDislike.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { 
                    boolean isLiked = prefs.getBoolean(currentDbKey + "_liked", false);
                    boolean isDisliked = prefs.getBoolean(currentDbKey + "_disliked", false);
                    if (isDisliked) {
                        recordStat("dislikes", -1, 0);
                        prefs.edit().putBoolean(currentDbKey + "_disliked", false).apply();
                        actionDislike.setIconResource(R.drawable.ic_thumb_down_outline);
                        try {
                            String txt = actionDislike.getText().toString();
                            int cur = txt.isEmpty() ? 0 : Integer.parseInt(txt);
                            actionDislike.setText(cur - 1 > 0 ? String.valueOf(cur - 1) : "");
                        } catch(Exception e) {}
                    } else {
                        recordStat("dislikes", 1, 0);
                        prefs.edit().putBoolean(currentDbKey + "_disliked", true).apply();
                        actionDislike.setIconResource(R.drawable.ic_thumb_down);
                        try {
                            String txt = actionDislike.getText().toString();
                            int cur = txt.isEmpty() ? 0 : Integer.parseInt(txt);
                            actionDislike.setText(String.valueOf(cur + 1));
                        } catch(Exception e) {}
                        
                        if (isLiked) {
                            recordStat("likes", -1, 0);
                            prefs.edit().putBoolean(currentDbKey + "_liked", false).apply();
                            actionLike.setIconResource(R.drawable.ic_thumb_up_outline);
                            try {
                                String txt = actionLike.getText().toString();
                                int cur = txt.isEmpty() ? 0 : Integer.parseInt(txt);
                                actionLike.setText(cur - 1 > 0 ? String.valueOf(cur - 1) : "");
                            } catch(Exception e) {}
                        }
                    }
                }
            });
            actionRateBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { showRatingDialog(); }
            });
        }
        
        java.util.List<Integer> seasons = getIntent().getIntegerArrayListExtra("available_seasons");
        if (seasons != null && !seasons.isEmpty() && playerSeasonsRow != null) {
            playerSeasonsScroll.setVisibility(View.VISIBLE);
            buildSeasonButtons(seasons, null);
        }
        
        View commentsPreview = findViewById(R.id.player_comments_preview_card);
        if (commentsPreview != null) {
            commentsPreview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCommentsBottomSheet();
                }
            });
        }
        
        loadNativeAd();
        // Banner ad removed for now
        
        onConfigurationChanged(getResources().getConfiguration());
        
        if (getIntent().getBooleanExtra("show_comments", false)) {
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> showCommentsBottomSheet(), 500);
        }
    }

    private void buildSeasonButtons(final java.util.List<Integer> seasons, final java.util.List<String> seasonNames) {
        playerSeasonsRow.removeAllViews();
        for (int i = 0; i < seasons.size(); i++) {
            final int sn = seasons.get(i);
            Button btn = new Button(this);
            btn.setText(seasonNames != null && i < seasonNames.size() && seasonNames.get(i) != null ? seasonNames.get(i) : "Season " + sn);
            btn.setTextSize(11f);
            btn.setAllCaps(false);
            btn.setBackgroundResource(sn == currentSeason ? R.drawable.bg_season_btn_active : R.drawable.bg_season_btn);
            btn.setTextColor(sn == currentSeason ? 0xFF000000 : 0xFFD1D5DB);
            btn.setTag(sn);
            btn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    currentSeason = sn;
                    for (int i = 0; i < playerSeasonsRow.getChildCount(); i++) {
                        View child = playerSeasonsRow.getChildAt(i);
                        if (child instanceof Button) {
                            int tagSn = (int) child.getTag();
                            child.setBackgroundResource(tagSn == sn ? R.drawable.bg_season_btn_active : R.drawable.bg_season_btn);
                            ((Button) child).setTextColor(tagSn == sn ? 0xFF000000 : 0xFFD1D5DB);
                        }
                    }
                    if (episodeAdapter != null) episodeAdapter.setItems(new java.util.ArrayList<Episode>());
                    loadEpisodesForSeason(sn);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(8);
            btn.setLayoutParams(lp);
            playerSeasonsRow.addView(btn);
        }
    }

    private void showEpisodesLoading(final boolean show) {
        runOnUiThread(() -> {
            if (playerEpisodesLoadingContainer != null) {
                playerEpisodesLoadingContainer.setVisibility(show ? View.VISIBLE : View.GONE);
                if (show && playerEpisodesLoadingGif != null) {
                    try {
                        com.bumptech.glide.Glide.with(PlayerActivity.this)
                            .asGif()
                            .load("https://raw.githubusercontent.com/zsecre/Watchable/refs/heads/main/loading.gif")
                            .error(com.bumptech.glide.Glide.with(PlayerActivity.this).asGif().load(R.raw.loading))
                            .into(playerEpisodesLoadingGif);
                    } catch (Exception ignored) {}
                }
            }
        });
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
                e.printStackTrace();
            }
        }).start();
    }

    private void loadEpisodesForSeason(int season) {
        if (episodeAdapter == null) return;
        showEpisodesLoading(true);
        if (episodeAdapter != null) episodeAdapter.setItems(new java.util.ArrayList<Episode>());
        
        if (animeMode != 0 && animeSeasonsMapping != null && item != null && !"Movie".equals(item.getType())) {
            int targetIndex = season - 1;
            if (targetIndex >= 0 && targetIndex < animeSeasonsMapping.length()) {
                String targetMalId = animeSeasonsMapping.optJSONObject(targetIndex).optString("mal_id");
                if (targetMalId != null && !targetMalId.isEmpty() && !"null".equals(targetMalId)) {
                    currentMalId = targetMalId;
                    fetchJikanEpisodes(targetMalId);
                    return;
                }
            }
        }

        if ((animeMode != 0 || (item != null && ("Anime".equals(item.getType()) || "jikan".equals(item.getSource())))) && item != null && !"Movie".equals(item.getType())) {
            String malIdToUse = currentMalId != null ? currentMalId : item.getId();
            if (malIdToUse != null && !malIdToUse.isEmpty() && !"null".equals(malIdToUse)) {
                fetchJikanEpisodes(malIdToUse);
                return;
            }
        }
        
        if (imdbId != null && !imdbId.isEmpty()) {
            ApiRepository.get().fetchEpisodesByImdb(imdbId, season, item.getPosterPath(), new ApiRepository.Callback<List<Episode>>() {
                public void onResult(List<Episode> eps) {
                    showEpisodesLoading(false);
                    episodeAdapter.setItems(eps);
                }
                public void onError(Exception e) {
                    showEpisodesLoading(false);
                }
            });
        } else {
            ApiRepository.get().fetchEpisodes(tmdbId != null ? tmdbId : item.getId(), season, new ApiRepository.Callback<List<Episode>>() {
                public void onResult(List<Episode> eps) {
                    showEpisodesLoading(false);
                    episodeAdapter.setItems(eps);
                }
                public void onError(Exception e) {
                    showEpisodesLoading(false);
                }
            });
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
    
    private void switchEpisodeMode(boolean isAnime) {
        if (item == null || "Movie".equals(item.getType())) return;
        java.util.List<Integer> seasonsToUse = new java.util.ArrayList<>();
        java.util.List<String> seasonNames = new java.util.ArrayList<>();
        if (isAnime && animeSeasonsMapping != null) {
            for (int i = 0; i < animeSeasonsMapping.length(); i++) {
                seasonsToUse.add(i + 1);
                String apId = animeSeasonsMapping.optJSONObject(i).optString("anime-planet_id");
                seasonNames.add(formatAnimeTitle(apId));
            }
        } else {
            java.util.List<Integer> origSeasons = getIntent().getIntegerArrayListExtra("available_seasons");
            if (origSeasons != null) {
                seasonsToUse.addAll(origSeasons);
            }
        }
        if (!seasonsToUse.contains(currentSeason) && !seasonsToUse.isEmpty()) {
            currentSeason = seasonsToUse.get(0);
        }
        if (playerSeasonsScroll != null && playerSeasonsRow != null) {
            if (seasonsToUse.isEmpty()) {
                playerSeasonsScroll.setVisibility(View.GONE);
            } else {
                playerSeasonsScroll.setVisibility(View.VISIBLE);
                buildSeasonButtons(seasonsToUse, isAnime ? seasonNames : null);
            }
        }
        if (episodeAdapter != null) episodeAdapter.setItems(new java.util.ArrayList<Episode>());
        loadEpisodesForSeason(currentSeason);
    }

    private void setupAnimeButtons(String malId) {
        currentMalId = malId;
        Button btnAnimeSub = findViewById(R.id.btn_anime_sub);
        Button btnAnimeDub = findViewById(R.id.btn_anime_dub);
        Button btnSelectServer = findViewById(R.id.btn_select_server);
        
        if (btnAnimeSub != null) {
            btnAnimeSub.setEnabled(true);
            btnAnimeSub.setTextColor(getResources().getColor(R.color.brand_cyan));
            btnAnimeSub.setOnClickListener(v -> {
                boolean changed = animeMode == 0;
                animeMode = 1;
                if (btnSelectServer != null) {
                    btnSelectServer.setText("Select Server");
                }
                loadVideo();
                if (changed) {
                    switchEpisodeMode(true);
                }
                btnAnimeSub.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF00FFFF));
                btnAnimeSub.setTextColor(getResources().getColor(R.color.bg_deep));
                if (btnAnimeDub != null) {
                    btnAnimeDub.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1F1F24));
                    btnAnimeDub.setTextColor(getResources().getColor(R.color.brand_cyan));
                }
            });
        }
        
        if (btnAnimeDub != null) {
            btnAnimeDub.setEnabled(true);
            btnAnimeDub.setTextColor(getResources().getColor(R.color.brand_cyan));
            btnAnimeDub.setOnClickListener(v -> {
                boolean changed = animeMode == 0;
                animeMode = 2;
                if (btnSelectServer != null) {
                    btnSelectServer.setText("Select Server");
                }
                loadVideo();
                if (changed) {
                    switchEpisodeMode(true);
                }
                btnAnimeDub.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF00FFFF));
                btnAnimeDub.setTextColor(getResources().getColor(R.color.bg_deep));
                if (btnAnimeSub != null) {
                    btnAnimeSub.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1F1F24));
                    btnAnimeSub.setTextColor(getResources().getColor(R.color.brand_cyan));
                }
            });
        }
        
        if (btnAnimeSub != null) {
            btnAnimeSub.performClick();
        }
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
                    String malId = null;
                    
                    if (item != null && "Movie".equals(item.getType())) {
                        malId = animeSeasonsMapping.getJSONObject(0).optString("mal_id");
                    } else {
                        int targetIndex = currentSeason - 1;
                        if (targetIndex >= 0 && targetIndex < animeSeasonsMapping.length()) {
                            malId = animeSeasonsMapping.getJSONObject(targetIndex).optString("mal_id");
                        } else {
                            malId = animeSeasonsMapping.getJSONObject(0).optString("mal_id");
                        }
                    }
                    
                    if (malId != null && !malId.isEmpty() && !malId.equals("null") && !malId.equals("0")) {
                        final String malIdFinal = malId;
                        runOnUiThread(() -> {
                            setupAnimeButtons(malIdFinal);
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void setupServers() {
        final Button btnSelectServer = findViewById(R.id.btn_select_server);
        if (btnSelectServer == null) return;
        
        final String[] serverNames = {"cinesrc", "vidfast", "vidsrc", "vidrock", "moviesapi.to"};
        
        btnSelectServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new android.app.AlertDialog.Builder(PlayerActivity.this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle("Select Server")
                    .setSingleChoiceItems(serverNames, selectedServer, new android.content.DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(android.content.DialogInterface dialog, int which) {
                            if (selectedServer != which || animeMode != 0) {
                                boolean changed = animeMode != 0;
                                selectedServer = which;
                                animeMode = 0;
                                Button btnSub = findViewById(R.id.btn_anime_sub);
                                Button btnDub = findViewById(R.id.btn_anime_dub);
                                if (btnSub != null && btnSub.isEnabled()) {
                                    btnSub.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1F1F24));
                                    btnSub.setTextColor(getResources().getColor(R.color.brand_cyan));
                                }
                                if (btnDub != null && btnDub.isEnabled()) {
                                    btnDub.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1F1F24));
                                    btnDub.setTextColor(getResources().getColor(R.color.brand_cyan));
                                }
                                if (selectedServer < serverNames.length) {
                                    btnSelectServer.setText("Server: " + serverNames[selectedServer]);
                                }
                                loadVideo();
                                if (changed) {
                                    switchEpisodeMode(false);
                                }
                            }
                            dialog.dismiss();
                        }
                    })
                    .show();
            }
        });
        
        if (selectedServer < serverNames.length) {
            btnSelectServer.setText("Server: " + serverNames[selectedServer]);
        }
    }
    
    private void startNativeAdRefreshTimer() {
        if (nativeAdRefreshHandler != null) {
            nativeAdRefreshHandler.removeCallbacks(nativeAdRefreshRunnable);
            nativeAdRefreshHandler.postDelayed(nativeAdRefreshRunnable, NATIVE_AD_REFRESH_INTERVAL_MS);
        }
    }

    private void loadNativeAd() {
        loadNativeAdWithId("ca-app-pub-7655017843258756/9261672689", false);
        startNativeAdRefreshTimer();
    }

    private void loadTopBannerAd() {
        final FrameLayout bannerContainer = findViewById(R.id.player_banner_container);
        if (bannerContainer == null) return;

        if (Math.random() < 0.60) {
            try {
                if (topAdView != null) {
                    topAdView.destroy();
                    topAdView = null;
                }
                topAdView = new AdView(this);
                topAdView.setAdUnitId("ca-app-pub-7655017843258756/9398267427");
                topAdView.setAdSize(AdSize.BANNER);
                topAdView.setAdListener(new com.google.android.gms.ads.AdListener() {
                    @Override
                    public void onAdLoaded() {
                        bannerContainer.setVisibility(View.VISIBLE);
                    }
                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        bannerContainer.setVisibility(View.GONE);
                    }
                });
                bannerContainer.removeAllViews();
                bannerContainer.addView(topAdView);
                topAdView.loadAd(new AdRequest.Builder().build());
            } catch (Exception e) {
                bannerContainer.setVisibility(View.GONE);
            }
        } else {
            bannerContainer.setVisibility(View.GONE);
        }
    }

    private void loadNativeAdWithId(final String adUnitId, final boolean fallbackToTest) {
        NativeAd.OnNativeAdLoadedListener listener = new NativeAd.OnNativeAdLoadedListener() {
            @Override
            public void onNativeAdLoaded(NativeAd nativeAd) {
                if (isDestroyed() || isFinishing()) {
                    nativeAd.destroy();
                    return;
                }

                FrameLayout frameLayout = findViewById(R.id.player_ad_container);
                if (frameLayout != null) {
                    for (int i = 0; i < frameLayout.getChildCount(); i++) {
                        View child = frameLayout.getChildAt(i);
                        if (child instanceof NativeAdView) {
                            ((NativeAdView) child).destroy();
                        }
                    }
                    frameLayout.removeAllViews();

                    if (currentNativeAd != null) {
                        currentNativeAd.destroy();
                    }
                    currentNativeAd = nativeAd;

                    NativeAdView adView = (NativeAdView) getLayoutInflater().inflate(R.layout.ad_native, frameLayout, false);
                    populateNativeAdView(nativeAd, adView);
                    frameLayout.addView(adView);
                } else {
                    nativeAd.destroy();
                    if (currentNativeAd != null) {
                        currentNativeAd.destroy();
                        currentNativeAd = null;
                    }
                }
            }
        };

        com.google.android.gms.ads.nativead.NativeAdOptions nativeAdOptions = 
            new com.google.android.gms.ads.nativead.NativeAdOptions.Builder()
                .setVideoOptions(new com.google.android.gms.ads.VideoOptions.Builder()
                    .setStartMuted(true)
                    .build())
                .setAdChoicesPlacement(com.google.android.gms.ads.nativead.NativeAdOptions.ADCHOICES_TOP_RIGHT)
                .build();

        AdLoader adLoader = new AdLoader.Builder(this, adUnitId)
            .forNativeAd(listener)
            .withNativeAdOptions(nativeAdOptions)
            .withAdListener(new com.google.android.gms.ads.AdListener() {
                @Override
                public void onAdFailedToLoad(com.google.android.gms.ads.LoadAdError adError) {
                    android.util.Log.e("PlayerActivity", "Ad failed to load (" + adUnitId + "): " + adError.getMessage());
                }
            })
            .build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private void showAdLoadingOverlay() {
        if (isFinishing() || isDestroyed()) return;
        runOnUiThread(() -> {
            if (adLoadingOverlay != null) {
                adLoadingOverlay.setVisibility(View.VISIBLE);
                if (adLoadingGif != null) {
                    try {
                        com.bumptech.glide.Glide.with(PlayerActivity.this)
                            .asGif()
                            .load("https://raw.githubusercontent.com/zsecre/Watchable/refs/heads/main/loading.gif")
                            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .error(com.bumptech.glide.Glide.with(PlayerActivity.this).asGif().load(R.raw.loading))
                            .into(adLoadingGif);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void hideAdLoadingOverlay() {
        runOnUiThread(() -> {
            if (adLoadingOverlay != null) {
                adLoadingOverlay.setVisibility(View.GONE);
                if (adLoadingGif != null) {
                    try {
                        com.bumptech.glide.Glide.with(PlayerActivity.this).clear(adLoadingGif);
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    private void playWithAd(final Runnable onComplete) {
        if (isFinishing() || isDestroyed()) return;
        
        long now = System.currentTimeMillis();
        if (now - lastInterstitialShownTime < INTERSTITIAL_COOLDOWN_MS) {
            // Cooldown active (20 seconds), bypass interstitial and proceed immediately
            if (!isFinishing() && !isDestroyed()) onComplete.run();
            return;
        }

        showAdLoadingOverlay();
        com.google.android.gms.ads.AdRequest adRequest = new com.google.android.gms.ads.AdRequest.Builder().build();
        com.google.android.gms.ads.interstitial.InterstitialAd.load(this, "ca-app-pub-7655017843258756/6152706304", adRequest,
            new com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@androidx.annotation.NonNull com.google.android.gms.ads.interstitial.InterstitialAd interstitialAd) {
                    if (isFinishing() || isDestroyed()) {
                        hideAdLoadingOverlay();
                        return;
                    }
                    interstitialAd.setFullScreenContentCallback(new com.google.android.gms.ads.FullScreenContentCallback(){
                        @Override
                        public void onAdShowedFullScreenContent() {
                            lastInterstitialShownTime = System.currentTimeMillis();
                            hideAdLoadingOverlay();
                        }
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            lastInterstitialShownTime = System.currentTimeMillis();
                            hideAdLoadingOverlay();
                            if (!isFinishing() && !isDestroyed()) onComplete.run();
                        }
                        @Override
                        public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) {
                            hideAdLoadingOverlay();
                            if (!isFinishing() && !isDestroyed()) onComplete.run();
                        }
                    });
                    interstitialAd.show(PlayerActivity.this);
                }

                @Override
                public void onAdFailedToLoad(@androidx.annotation.NonNull com.google.android.gms.ads.LoadAdError loadAdError) {
                    hideAdLoadingOverlay();
                    if (!isFinishing() && !isDestroyed()) onComplete.run();
                }
            });
    }

    private void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
        adView.setMediaView((MediaView) adView.findViewById(R.id.ad_media));
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

        ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        ((MediaView) adView.getMediaView()).setMediaContent(nativeAd.getMediaContent());

        if (nativeAd.getBody() == null) {
            adView.getBodyView().setVisibility(View.INVISIBLE);
        } else {
            adView.getBodyView().setVisibility(View.VISIBLE);
            ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        }

        if (nativeAd.getCallToAction() == null) {
            adView.getCallToActionView().setVisibility(View.INVISIBLE);
        } else {
            adView.getCallToActionView().setVisibility(View.VISIBLE);
            ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
        }

        if (nativeAd.getIcon() == null) {
            adView.getIconView().setVisibility(View.GONE);
        } else {
            ((ImageView) adView.getIconView()).setImageDrawable(
                    nativeAd.getIcon().getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getAdvertiser() == null) {
            adView.getAdvertiserView().setVisibility(View.INVISIBLE);
        } else {
            ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
            adView.getAdvertiserView().setVisibility(View.VISIBLE);
        }

        adView.setNativeAd(nativeAd);
    }

    // Rest of your existing code (loadVideo, setupEpisodes, etc.) remains unchanged
    private void loadVideo() {
        if (playerWebView != null) {
            playerWebView.stopLoading();
        }
        String url = "";
        String tId = tmdbId != null ? tmdbId : item.getId();
        String idToUse = imdbId != null && !imdbId.isEmpty() ? imdbId : tId;
        
        if (animeMode == 1 && currentMalId != null) {
            int ep = "Movie".equals(item.getType()) ? 1 : currentEpisode;
            url = "https://megaplay.buzz/stream/mal/" + currentMalId + "/" + ep + "/sub?allowfullscreen=false";
        } else if (animeMode == 2 && currentMalId != null) {
            int ep = "Movie".equals(item.getType()) ? 1 : currentEpisode;
            url = "https://megaplay.buzz/stream/mal/" + currentMalId + "/" + ep + "/dub?allowfullscreen=false";
        } else if (selectedServer == 0) {
            if ("Movie".equals(item.getType())) {
                url = "https://cinesrc.st/embed/movie/" + tId;
            } else {
                url = "https://cinesrc.st/embed/tv/" + tId + "?s=" + currentSeason + "&e=" + currentEpisode;
            }
        } else if (selectedServer == 1) {
            if ("Movie".equals(item.getType())) {
                url = "https://www.vidfast.pro/movie/" + idToUse + "?autoPlay=true&server=vEdge&sub=en";
            } else {
                url = "https://www.vidfast.pro/tv/" + idToUse + "/" + currentSeason + "/" + currentEpisode + "?autoPlay=true&server=vEdge&sub=en";
            }
        } else if (selectedServer == 2) {
            if ("Movie".equals(item.getType())) {
                url = imdbId != null && !imdbId.isEmpty() ? "https://vsembed.ru/embed/movie/" + imdbId
                    : "https://vsembed.ru/embed/movie?tmdb=" + tId;
            } else {
                url = imdbId != null && !imdbId.isEmpty()
                    ? "https://vsembed.ru/embed/tv/" + imdbId + "/" + currentSeason + "/" + currentEpisode
                    : "https://vsembed.ru/embed/tv?tmdb=" + tId + "&season=" + currentSeason + "&episode=" + currentEpisode;
            }
        } else if (selectedServer == 3) {
            if ("Movie".equals(item.getType())) {
                url = "https://vidrock.to/movie/" + tId + "?autoplay=true&lang=en&theme=00FFFF";
            } else {
                url = "https://vidrock.to/embed/tv/" + tId + "/" + currentSeason + "/" + currentEpisode + "?autoplay=true&lang=en&theme=00FFFF";
            }
        } else if (selectedServer == 4) {
            if ("Movie".equals(item.getType())) {
                url = "https://moviesapi.to/movie/" + tId;
            } else {
                url = "https://moviesapi.to/tv/" + tId + "-" + currentSeason + "-" + currentEpisode;
            }
        } else {
            if ("Movie".equals(item.getType())) {
                url = "https://cinesrc.st/embed/movie/" + tId;
            } else {
                url = "https://cinesrc.st/embed/tv/" + tId + "?s=" + currentSeason + "&e=" + currentEpisode;
            }
        }
        
        java.util.Map<String, String> extraHeaders = new java.util.HashMap<>();
        if (!url.isEmpty()) {
            extraHeaders.put("Referer", url);
            try {
                java.net.URI uri = new java.net.URI(url);
                String origin = uri.getScheme() + "://" + uri.getHost();
                extraHeaders.put("Origin", origin);
            } catch (Exception ignored) {
                if (url.length() > 8 && url.indexOf('/', 8) != -1) {
                    extraHeaders.put("Origin", url.substring(0, url.indexOf('/', 8)));
                }
            }
            playerWebView.loadUrl(url, extraHeaders);
        }
        initStatsForCurrentVideo();
    }

    private void checkUrlForEpisodeChange(String url) {
        if (item == null || "Movie".equals(item.getType())) return;
        if (url == null) return;
        
        int newSeason = currentSeason;
        int newEpisode = currentEpisode;
        
        java.util.regex.Pattern pattern1 = java.util.regex.Pattern.compile("(?:/tv/|/embed/tv/)[A-Za-z0-9_-]+[-/](\\d+)[-/](\\d+)");
        java.util.regex.Matcher m1 = pattern1.matcher(url);
        if (m1.find()) {
            try {
                newSeason = Integer.parseInt(m1.group(1));
                newEpisode = Integer.parseInt(m1.group(2));
            } catch (Exception e) {}
        } else {
            java.util.regex.Pattern pattern2 = java.util.regex.Pattern.compile("[?&](?:season|s)=(\\d+)&(?:episode|e)=(\\d+)");
            java.util.regex.Matcher m2 = pattern2.matcher(url);
            if (m2.find()) {
                try {
                    newSeason = Integer.parseInt(m2.group(1));
                    newEpisode = Integer.parseInt(m2.group(2));
                } catch (Exception e) {}
            }
        }
        
        if (newSeason > 0 && newEpisode > 0 && (newSeason != currentSeason || newEpisode != currentEpisode)) {
            currentSeason = newSeason;
            currentEpisode = newEpisode;
            
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (nowPlayingSub != null) {
                        nowPlayingSub.setText("S" + currentSeason + " E" + currentEpisode);
                    }
                    if (playerEpisodeInput != null) {
                        playerEpisodeInput.setText(String.valueOf(currentEpisode));
                    }
                    if (episodeAdapter != null) {
                        episodeAdapter.notifyDataSetChanged();
                    }
                    initStatsForCurrentVideo();
                }
            });
        }
    }

    private void setupSimilarShows() {
        final View similarSection = findViewById(R.id.player_similar_section);
        final RecyclerView similarRv = findViewById(R.id.player_similar_rv);
        if (similarSection == null || similarRv == null) return;
        
        TextView title = findViewById(R.id.player_similar_title);
        if (title != null) title.setText("Movie".equals(item.getType()) ? "Similar Movies" : "Similar Shows");

        ApiRepository.get().fetchDetails(item, new ApiRepository.Callback<MediaDetails>() {
            @Override
            public void onResult(final MediaDetails details) {
                if (details.getImdbId() != null) {
                    loadImdbRating(details.getImdbId());
                }
                if (details.getSimilar() != null && !details.getSimilar().isEmpty()) {
                    similarSection.setVisibility(View.VISIBLE);
                    similarRv.setLayoutManager(new LinearLayoutManager(PlayerActivity.this, LinearLayoutManager.HORIZONTAL, false));
                    similarRv.setAdapter(new MediaRowAdapter(details.getSimilar(), new MediaRowAdapter.OnItemClick() {
                        public void onClick(MediaItem other) { openDetails(other); }
                    }));
                }
            }
            @Override
            public void onError(Exception e) {}
        });
    }

    private void loadImdbRating(String id) {
        if (id == null || id.isEmpty()) return;
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url("https://api.tiffara.com/titles/" + id)
                        .build();
                Response response = new OkHttpClient().newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    if (json.has("rating")) {
                        JSONObject ratingObj = json.getJSONObject("rating");
                        final double aggregateRating = ratingObj.optDouble("aggregateRating", 0);
                        runOnUiThread(() -> {
                            if (statImdbRating != null && aggregateRating > 0) {
                                statImdbRating.setText(aggregateRating > 0 ? String.format("IMDB Ratings: %.1f", aggregateRating) : "IMDB Ratings: N/A");
                            }
                        });
                    }
                }
            } catch (Exception e) {}
        }).start();
    }

    private void showRatingDialog() {
        if (item == null) return;
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_rating);
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        
        TextView titleView = dialog.findViewById(R.id.rating_title);
        android.widget.RatingBar ratingBar = dialog.findViewById(R.id.dialog_rating_bar);
        Button btnClose = dialog.findViewById(R.id.btn_close);
        
        String titleText = "Rate " + item.getTitle();
        if ("TV Show".equals(item.getType())) {
            titleText += " (S" + currentSeason + " E" + currentEpisode + ")";
        }
        titleView.setText(titleText);
        
        btnClose.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { dialog.dismiss(); }
        });
        
        int oldRating = prefs.getInt(currentDbKey + "_rating", 0);
        if (oldRating > 0) {
            ratingBar.setRating(oldRating);
        }

        ratingBar.setOnRatingBarChangeListener(new android.widget.RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(android.widget.RatingBar bar, float rating, boolean fromUser) {
                if (fromUser) {
                    int currentO = prefs.getInt(currentDbKey + "_rating", 0);
                    recordStat("rating", (int)rating, currentO);
                    prefs.edit().putInt(currentDbKey + "_rating", (int)rating).apply();
                    actionRateBtn.setText("" + (int)rating);
                    actionRateBtn.setIconResource(R.drawable.ic_star);
                    dialog.dismiss();
                }
            }
        });
        
        dialog.show();
    }

    private void openDetails(MediaItem other) {
        android.content.Intent i = new android.content.Intent(this, DetailsActivity.class);
        i.putExtra(Constants.EXTRA_ITEM, other);
        startActivity(i);
        finish();
    }

    private void setupEpisodes() {
        final RecyclerView rv = findViewById(R.id.player_episodes_rv);
        final View epSection = findViewById(R.id.player_episodes_section);
        
        if (playerEpisodeGo != null) {
            playerEpisodeGo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (playerEpisodeInput != null) {
                        String val = playerEpisodeInput.getText().toString();
                        if (!val.isEmpty()) {
                            try {
                                int pos = Integer.parseInt(val) - 1;
                                if (episodeAdapter != null && pos >= 0) {
                                    if (pos < episodeAdapter.getItemCount()) {
                                        rv.scrollToPosition(pos);
                                        currentEpisode = pos + 1;
                                        playWithAd(new Runnable() {
                                            @Override
                                            public void run() {
                                                loadVideo();
                                                updateNowPlaying();
                                                if (episodeAdapter != null) episodeAdapter.setPlayingEpisode(currentSeason, currentEpisode);
                                                loadNativeAd();
                                            }
                                        });
                                    } else {
                                        android.widget.Toast.makeText(PlayerActivity.this, "Episode not loaded yet", android.widget.Toast.LENGTH_SHORT).show();
                                    }
                                }
                            } catch (Exception e) {}
                        }
                    }
                }
            });
        }
        if (!"TV Show".equals(item.getType()) && !"Anime".equals(item.getType())) {
            epSection.setVisibility(View.GONE); return;
        }
        epSection.setVisibility(View.VISIBLE);
        episodeAdapter = new EpisodeAdapter(new EpisodeAdapter.OnClick() {
            public void onClick(Episode ep) {
                currentEpisode = ep.getEpisodeNumber();
                playWithAd(new Runnable() {
                    @Override
                    public void run() {
                        loadVideo();
                        updateNowPlaying();
                        episodeAdapter.setPlayingEpisode(currentSeason, currentEpisode);
                        loadNativeAd();
                    }
                });
            }
        });
        episodeAdapter.setPosterPath(item.getPosterPath());
        episodeAdapter.setPlayingEpisode(currentSeason, currentEpisode);
        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rv.setAdapter(episodeAdapter);
        loadEpisodesForSeason(currentSeason);
    }

    private void updateNowPlaying() {
        nowPlayingTitle.setText(item.getTitle());
        String sub = item.getType();
        if (!"Movie".equals(item.getType())) sub += " · Season " + currentSeason + " Ep " + currentEpisode;
        nowPlayingSub.setText(sub);
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        View scroll = findViewById(R.id.player_scroll);
        View frame = findViewById(R.id.player_video_frame);
        
        if (newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            if (customVideoView == null) {
                if (scroll != null) scroll.setVisibility(View.GONE);
                if (frame != null) {
                    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) frame.getLayoutParams();
                    lp.width = LinearLayout.LayoutParams.MATCH_PARENT;
                    lp.height = LinearLayout.LayoutParams.MATCH_PARENT;
                    lp.weight = 1;
                    frame.setLayoutParams(lp);
                }
                if (playerWebView != null) {
                    android.view.ViewGroup.LayoutParams wlp = playerWebView.getLayoutParams();
                    wlp.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                    wlp.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                    playerWebView.setLayoutParams(wlp);
                }
            }
        } else {
            if (customVideoView == null) {
                if (scroll != null) scroll.setVisibility(View.VISIBLE);
                if (frame != null) {
                    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) frame.getLayoutParams();
                    lp.width = LinearLayout.LayoutParams.MATCH_PARENT;
                    lp.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    lp.weight = 0;
                    frame.setLayoutParams(lp);
                }
                if (playerWebView != null) {
                    android.view.ViewGroup.LayoutParams wlp = playerWebView.getLayoutParams();
                    wlp.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                    wlp.height = (int) (220 * getResources().getDisplayMetrics().density);
                    playerWebView.setLayoutParams(wlp);
                }
            }
        }
    }

    private void initStatsForCurrentVideo() {
        if (item == null) return;
        
        if (currentMalId != null && !currentMalId.isEmpty() && !currentMalId.equals("null")) {
            if ("Movie".equals(item.getType())) {
                currentDbKey = "ANIME_MOVIE_" + currentMalId;
            } else {
                currentDbKey = "ANIME_TV_" + currentMalId + "_E" + currentEpisode;
            }
        } else if ("jikan".equals(item.getSource()) || "Anime".equals(item.getType())) {
            String malId = item.getId();
            if ("Movie".equals(item.getType())) {
                currentDbKey = "ANIME_MOVIE_" + malId;
            } else {
                currentDbKey = "ANIME_TV_" + malId + "_E" + currentEpisode;
            }
        } else {
            String tId = tmdbId != null ? tmdbId : item.getId();
            String idToUse = imdbId != null && !imdbId.isEmpty() ? imdbId : tId;
            if ("Movie".equals(item.getType())) {
                currentDbKey = "MOVIE_" + idToUse;
            } else {
                currentDbKey = "TV_" + idToUse + "_S" + currentSeason + "_E" + currentEpisode;
            }
        }

        viewRecorded = false;
        statsHandler.removeCallbacksAndMessages(null);
        statsHandler.postDelayed(viewRunnable, 60000);
        
        if (actionLike != null) {
            actionLike.setEnabled(true);
            actionLike.setText("");
            actionDislike.setEnabled(true);
            actionDislike.setText("");
            if (actionRateBtn != null) {
                actionRateBtn.setEnabled(true);
                int r = prefs.getInt(currentDbKey + "_rating", 0);
                if (r > 0) {
                    actionRateBtn.setText("" + r);
                    actionRateBtn.setIconResource(R.drawable.ic_star);
                } else {
                    actionRateBtn.setText("Rate");
                    actionRateBtn.setIconResource(R.drawable.ic_star_outline);
                }
            }
            boolean isLiked = prefs.getBoolean(currentDbKey + "_liked", false);
            boolean isDisliked = prefs.getBoolean(currentDbKey + "_disliked", false);
            actionLike.setIconResource(isLiked ? R.drawable.ic_thumb_up : R.drawable.ic_thumb_up_outline);
            actionDislike.setIconResource(isDisliked ? R.drawable.ic_thumb_down : R.drawable.ic_thumb_down_outline);
            if (statViews != null) statViews.setText("Views: ...");
            if (statRating != null) statRating.setText("WatchNinja Ratings: ...");
        }

        fetchStats();
        fetchComments();
    }

    private void fetchStats() {
        if (currentDbKey.isEmpty()) return;
        DatabaseService.get().fetchStats(currentDbKey, getUniqueUserId(), new DatabaseService.DataCallback<DatabaseService.StatsData>() {
            @Override
            public void onSuccess(DatabaseService.StatsData data) {
                if (statViews != null) {
                    statViews.setText("Views: " + data.views);
                    if (statLikes != null) statLikes.setText("" + data.likes);
                    if (statDislikes != null) statDislikes.setText("" + data.dislikes);
                    if (statRating != null) statRating.setText(data.ratingAverage > 0 ? String.format("WatchNinja Ratings: %.1f", data.ratingAverage) : "WatchNinja Ratings: N/A");
                }
                if (actionLike != null) {
                    actionLike.setText(data.likes > 0 ? String.valueOf(data.likes) : "");
                }
                if (actionDislike != null) {
                    actionDislike.setText(data.dislikes > 0 ? String.valueOf(data.dislikes) : "");
                }
            }

            @Override
            public void onError(Exception e) {}
        });
    }

    private String getUniqueUserId() {
        return AppPreferences.get(this).getUserId();
    }

    private void recordStat(final String type, final int value, final int oldRating) {
        if (currentDbKey.isEmpty()) return;
        
        final boolean isCurrentlyLiked = prefs.getBoolean(currentDbKey + "_liked", false);
        final boolean isCurrentlyDisliked = prefs.getBoolean(currentDbKey + "_disliked", false);
        String myName = AppPreferences.get(this).getUserName();

        String action = type;
        Integer ratingVal = "rating".equals(type) ? value : null;
        Boolean likeVal = ("likes".equals(type) || "dislikes".equals(type) || "like_dislike".equals(type)) ? isCurrentlyLiked : null;
        Boolean dislikeVal = ("likes".equals(type) || "dislikes".equals(type) || "like_dislike".equals(type)) ? isCurrentlyDisliked : null;

        DatabaseService.get().recordAction(currentDbKey, action, getUniqueUserId(), myName, ratingVal, likeVal, dislikeVal,
            new DatabaseService.DataCallback<DatabaseService.StatsData>() {
                @Override
                public void onSuccess(DatabaseService.StatsData data) {
                    fetchStats();
                }

                @Override
                public void onError(Exception e) {}
            });
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (topAdView != null) {
            topAdView.pause();
        }
        if (playerWebView != null) {
            playerWebView.onPause();
        }
        if (nativeAdRefreshHandler != null) {
            nativeAdRefreshHandler.removeCallbacks(nativeAdRefreshRunnable);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (topAdView != null) {
            topAdView.resume();
        }
        if (playerWebView != null) {
            playerWebView.onResume();
        }
        startNativeAdRefreshTimer();
    }

    private BottomSheetDialog commentsDialog;
    private CommentAdapter commentAdapter;
    private Comment replyingTo = null;
    private int commentSortMode = 0; // 0 = Newest, 1 = Top
    private List<Comment> currentCommentsList = new java.util.ArrayList<>();

    private void sortAndApplyComments() {
        if (currentCommentsList == null) return;
        if (commentSortMode == 1) {
            // Top: highest likes on top, lowest likes bottom. Secondary sort newest first
            java.util.Collections.sort(currentCommentsList, (a, b) -> {
                int diff = Integer.compare(b.getLikeCount(), a.getLikeCount());
                if (diff != 0) return diff;
                return b.getId().compareTo(a.getId());
            });
        } else {
            // New: newest comment on top (timestamp descending)
            java.util.Collections.sort(currentCommentsList, (a, b) -> b.getId().compareTo(a.getId()));
        }
        if (commentAdapter != null) {
            commentAdapter.setComments(new java.util.ArrayList<>(currentCommentsList));
        }
        updateCommentSortButtonsUI();
    }

    private void updateCommentSortButtonsUI() {
        if (commentsDialog != null) {
            TextView btnNewest = commentsDialog.findViewById(R.id.btn_sort_newest);
            TextView btnTop = commentsDialog.findViewById(R.id.btn_sort_top);
            if (btnNewest != null && btnTop != null) {
                if (commentSortMode == 0) {
                    btnNewest.setBackgroundResource(R.drawable.bg_pill_cyan);
                    btnNewest.setTextColor(0xFF000000);
                    btnTop.setBackgroundResource(R.drawable.bg_pill_grey);
                    btnTop.setTextColor(0xFFAAAAAA);
                } else {
                    btnTop.setBackgroundResource(R.drawable.bg_pill_cyan);
                    btnTop.setTextColor(0xFF000000);
                    btnNewest.setBackgroundResource(R.drawable.bg_pill_grey);
                    btnNewest.setTextColor(0xFFAAAAAA);
                }
            }
        }
    }

    private void showCommentsBottomSheet() {
        if (commentsDialog == null) {
            commentsDialog = new BottomSheetDialog(this);
            View view = getLayoutInflater().inflate(R.layout.bottom_sheet_comments, null);
            commentsDialog.setContentView(view);
            
            android.view.Window window = commentsDialog.getWindow();
            if (window != null) {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            }

            RecyclerView recycler = view.findViewById(R.id.comments_recycler);
            recycler.setLayoutManager(new LinearLayoutManager(this));
            
            TextView btnNewest = view.findViewById(R.id.btn_sort_newest);
            TextView btnTop = view.findViewById(R.id.btn_sort_top);
            if (btnNewest != null) {
                btnNewest.setOnClickListener(v -> {
                    if (commentSortMode != 0) {
                        commentSortMode = 0;
                        sortAndApplyComments();
                    }
                });
            }
            if (btnTop != null) {
                btnTop.setOnClickListener(v -> {
                    if (commentSortMode != 1) {
                        commentSortMode = 1;
                        sortAndApplyComments();
                    }
                });
            }

            commentAdapter = new CommentAdapter(getUniqueUserId(), new CommentAdapter.OnCommentActionListener() {
                @Override
                public void onReply(Comment comment) {
                    replyingTo = comment;
                    android.widget.EditText input = view.findViewById(R.id.comment_input);
                    input.setHint("Replying to " + comment.getAuthorName() + "...");
                    input.requestFocus();
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                }

                @Override
                public void onLike(Comment comment, boolean isLikeAction) {
                    if (prefs != null) {
                        prefs.edit()
                             .putBoolean("comment_" + comment.getId() + "_liked", comment.isLiked())
                             .putBoolean("comment_" + comment.getId() + "_disliked", comment.isDisliked())
                             .apply();
                    }
                    if (isLikeAction && comment.isLiked()) {
                        NotificationHelper.sendNotification(PlayerActivity.this, DB_URL, item, "Movie".equals(item.getType()) ? null : currentSeason, "Movie".equals(item.getType()) ? null : currentEpisode, comment.getAuthorId(), "like", "liked", imdbId, tmdbId);
                    } else if (!isLikeAction && comment.isDisliked()) {
                        NotificationHelper.sendNotification(PlayerActivity.this, DB_URL, item, "Movie".equals(item.getType()) ? null : currentSeason, "Movie".equals(item.getType()) ? null : currentEpisode, comment.getAuthorId(), "dislike", "disliked", imdbId, tmdbId);
                    }
                    
                    if (commentSortMode == 1) {
                        sortAndApplyComments();
                    }
                    
                    DatabaseService.get().updateComment(currentDbKey, comment.getId(), comment.getParentId(), null, comment.getLikeCount(), null);
                }
                
                @Override
                public void onEdit(Comment comment, boolean isReply, Comment parentComment) {
                    android.widget.EditText input = view.findViewById(R.id.comment_input);
                    input.setText(comment.getText());
                    input.setSelection(comment.getText().length());
                    input.requestFocus();
                    input.setTag(new Object[]{comment, parentComment});
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                }

                @Override
                public void onDelete(Comment comment, boolean isReply, Comment parentComment) {
                    deleteFirebaseComment(comment, parentComment);
                }
            });
            recycler.setAdapter(commentAdapter);
            
            android.widget.EditText input = view.findViewById(R.id.comment_input);
            view.findViewById(R.id.btn_send_comment).setOnClickListener(v -> {
                String text = input.getText().toString().trim();
                if (!text.isEmpty()) {
                    Object tag = input.getTag();
                    if (tag != null && tag instanceof Object[]) {
                        // Edit existing comment
                        Object[] t = (Object[]) tag;
                        Comment editing = (Comment) t[0];
                        Comment parent = (Comment) t[1];
                        editing.setText(text);
                        saveFirebaseComment(editing, parent);
                        commentAdapter.notifyDataSetChanged();
                        input.setTag(null);
                    } else {
                        String uid = getUniqueUserId();
                        String username = AppPreferences.get(PlayerActivity.this).getUserName();
                        if (username == null || username.isEmpty()) {
                            username = "User-" + uid.substring(0, 6);
                        }
                        String avatarUrl = AppPreferences.get(PlayerActivity.this).getUserAvatar();
                        Comment newComment = new Comment(String.valueOf(System.currentTimeMillis()), uid, username, avatarUrl, String.valueOf(System.currentTimeMillis()), text, 0);
                        if (replyingTo != null) {
                            replyingTo.addReply(newComment);
                            saveFirebaseComment(newComment, replyingTo);
                            NotificationHelper.sendNotification(PlayerActivity.this, DB_URL, item, "Movie".equals(item.getType()) ? null : currentSeason, "Movie".equals(item.getType()) ? null : currentEpisode, replyingTo.getAuthorId(), "reply", "replied to", imdbId, tmdbId);
                            commentAdapter.notifyDataSetChanged();
                            replyingTo = null;
                            input.setHint("Add a comment...");
                            fetchComments();
                        } else {
                            saveFirebaseComment(newComment, null);
                            currentCommentsList.add(0, newComment);
                            sortAndApplyComments();
                            recycler.smoothScrollToPosition(0);
                            fetchComments();
                        }
                    }
                    input.setText("");
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                }
            });
            
            commentsDialog.setOnDismissListener(dialog -> {
                replyingTo = null;
                input.setTag(null);
                input.setHint("Add a comment...");
                input.setText("");
            });
            commentsDialog.setOnShowListener(dialog -> {
                com.google.android.material.bottomsheet.BottomSheetDialog d = (com.google.android.material.bottomsheet.BottomSheetDialog) dialog;
                View bottomSheetInternal = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (bottomSheetInternal != null) {
                    int videoHeight = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 220, getResources().getDisplayMetrics());
                    int statusBarHeight = 0;
                    int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
                    if (resourceId > 0) {
                        statusBarHeight = getResources().getDimensionPixelSize(resourceId);
                    }
                    int maxHeight = getResources().getDisplayMetrics().heightPixels - videoHeight - statusBarHeight;
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheetInternal).setPeekHeight(maxHeight);
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheetInternal).setMaxHeight(maxHeight);
                    android.view.ViewGroup.LayoutParams layoutParams = bottomSheetInternal.getLayoutParams();
                    layoutParams.height = maxHeight;
                    bottomSheetInternal.setLayoutParams(layoutParams);
                }
            });
        }
        
        updateCommentSortButtonsUI();
        fetchComments();
        if (commentAdapter != null) {
            commentAdapter.notifyDataSetChanged();
        }
        
        // Update user avatar in the bottom sheet input UI
        if (commentsDialog != null) {
            android.widget.ImageView avatarImg = commentsDialog.findViewById(R.id.current_user_avatar_img);
            android.widget.TextView avatarText = commentsDialog.findViewById(R.id.current_user_avatar_text);
            if (avatarImg != null && avatarText != null) {
                String myName = AppPreferences.get(this).getUserName();
                String avatarUrl = AppPreferences.get(this).getUserAvatar();
                
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    avatarImg.setVisibility(View.VISIBLE);
                    avatarText.setVisibility(View.GONE);
                    com.bumptech.glide.Glide.with(this).load(avatarUrl).circleCrop().into(avatarImg);
                } else {
                    avatarImg.setVisibility(View.GONE);
                    avatarText.setVisibility(View.VISIBLE);
                    if (myName != null && !myName.isEmpty()) {
                        avatarText.setText(myName.substring(0, 1).toUpperCase());
                    } else {
                        avatarText.setText("U");
                    }
                }
            }
        }
        
        commentsDialog.show();
    }

    private void fetchComments() {
        if (currentDbKey == null || currentDbKey.isEmpty()) return;
        DatabaseService.get().fetchComments(currentDbKey, new DatabaseService.DataCallback<List<Comment>>() {
            @Override
            public void onSuccess(List<Comment> result) {
                currentCommentsList = result;
                sortAndApplyComments();
                int count = result.size();
                for (Comment c : result) count += c.getReplies().size();
                TextView titleTv = findViewById(R.id.player_comments_count);
                TextView descTv = findViewById(R.id.player_comments_text);
                if (titleTv != null) titleTv.setText("Comments  " + count);
                if (descTv != null && result.size() > 0) {
                    Comment latest = result.get(0);
                    descTv.setText(latest.getText());
                    
                    android.widget.ImageView previewImg = findViewById(R.id.player_comments_preview_avatar_img);
                    android.widget.TextView previewText = findViewById(R.id.player_comments_preview_avatar_text);
                    if (previewImg != null && previewText != null) {
                        String dispName = latest.getAuthorName();
                        String avatarUrl = latest.getAuthorAvatar();
                        
                        String currentUid = getUniqueUserId();
                        if (currentUid != null && currentUid.equals(latest.getAuthorId())) {
                            String myName = AppPreferences.get(PlayerActivity.this).getUserName();
                            if (myName != null && !myName.isEmpty()) dispName = myName;
                            avatarUrl = AppPreferences.get(PlayerActivity.this).getUserAvatar();
                        }
                        
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            previewImg.setVisibility(View.VISIBLE);
                            previewText.setVisibility(View.GONE);
                            com.bumptech.glide.Glide.with(PlayerActivity.this).load(avatarUrl).circleCrop().into(previewImg);
                        } else {
                            previewImg.setVisibility(View.GONE);
                            previewText.setVisibility(View.VISIBLE);
                            if (dispName != null && !dispName.isEmpty()) {
                                previewText.setText(dispName.substring(0, 1).toUpperCase());
                            } else {
                                previewText.setText("U");
                            }
                        }
                    }
                }
            }

            @Override
            public void onError(Exception e) {}
        });
    }

    private void saveFirebaseComment(Comment c, Comment parent) {
        if (currentDbKey == null || currentDbKey.isEmpty()) return;
        DatabaseService.get().saveComment(currentDbKey, c, parent, null);
    }

    private void deleteFirebaseComment(Comment c, Comment parent) {
        if (currentDbKey == null || currentDbKey.isEmpty()) return;
        DatabaseService.get().deleteComment(currentDbKey, c, parent, null);
    }

    @Override
    protected void onDestroy() {
        if (nativeAdRefreshHandler != null) {
            nativeAdRefreshHandler.removeCallbacksAndMessages(null);
        }
        FrameLayout bannerContainer = findViewById(R.id.player_banner_container);
        if (bannerContainer != null) {
            bannerContainer.removeAllViews();
        }
        if (topAdView != null) {
            topAdView.destroy();
            topAdView = null;
        }

        FrameLayout adContainer = findViewById(R.id.player_ad_container);
        if (adContainer != null) {
            for (int i = 0; i < adContainer.getChildCount(); i++) {
                android.view.View child = adContainer.getChildAt(i);
                if (child instanceof com.google.android.gms.ads.nativead.NativeAdView) {
                    ((com.google.android.gms.ads.nativead.NativeAdView) child).destroy();
                }
            }
            adContainer.removeAllViews();
        }
        if (currentNativeAd != null) {
            currentNativeAd.destroy();
            currentNativeAd = null;
        }

        if (playerWebView != null) {
            android.view.ViewGroup parent = (android.view.ViewGroup) playerWebView.getParent();
            if (parent != null) {
                parent.removeView(playerWebView);
            }
            playerWebView.removeAllViews();
            playerWebView.stopLoading();
            playerWebView.clearHistory();
            playerWebView.clearCache(true);
            playerWebView.destroy();
            playerWebView = null;
        }
        handler.removeCallbacksAndMessages(null);
        if (statsHandler != null) {
            statsHandler.removeCallbacksAndMessages(null);
        }
        super.onDestroy();
    }
}

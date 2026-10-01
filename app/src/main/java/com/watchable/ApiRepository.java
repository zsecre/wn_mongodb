package com.watchable;

import android.os.Handler;
import android.os.Looper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ApiRepository {
    private static ApiRepository instance;
    private final OkHttpClient client;
    private final Handler main = new Handler(Looper.getMainLooper());

    public interface Callback<T> {
        void onResult(T result);
        void onError(Exception e);
    }

    private ApiRepository() {
        client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS).build();
    }

    public static ApiRepository get() {
        if (instance == null) instance = new ApiRepository();
        return instance;
    }

    private String tmdb(String path) {
        return Constants.TMDB_BASE + path + (path.contains("?") ? "&" : "?") + "api_key=" + Constants.TMDB_KEY;
    }
    private String jikan(String path) {
        return Constants.JIKAN_BASE + path + (path.contains("?") ? "&" : "?") + "key=" + Constants.VAL_KEY;
    }

    private void fetchJson(final String url, final Callback<JsonObject> cb) {
        Request req = new Request.Builder().url(url).addHeader("User-Agent","Mozilla/5.0").build();
        doFetchJson(req, cb, 0);
    }
    
    private void doFetchJson(final Request req, final Callback<JsonObject> cb, final int retryCount) {
        client.newCall(req).enqueue(new okhttp3.Callback() {
            public void onFailure(Call call, final IOException e) {
                main.post(new Runnable() { public void run() { cb.onError(e); } });
            }
            public void onResponse(Call call, Response response) throws IOException {
                final String body = response.body() != null ? response.body().string() : "";
                final int code = response.code();
                if ((code == 429 || code == 504 || code == 500) && retryCount < 3) {
                    main.postDelayed(new Runnable() { public void run() { doFetchJson(req, cb, retryCount + 1); } }, 1000 * (retryCount + 1));
                    return;
                }
                main.post(new Runnable() {
                    public void run() {
                        if (code < 200 || code >= 300) { cb.onError(new Exception("HTTP "+code)); return; }
                        try {
                            cb.onResult(new JsonParser().parse(body).getAsJsonObject());
                        } catch (Exception e) { cb.onError(e); }
                    }
                });
            }
        });
    }

    private MediaItem fmtTmdb(JsonObject o, String type) {
        String id = (o.has("id") && !o.get("id").isJsonNull()) ? String.valueOf(o.get("id").getAsLong()) : "";
        String title = gs(o,"title",gs(o,"name","Untitled"));
        String poster = o.has("poster_path") && !o.get("poster_path").isJsonNull()
            ? Constants.IMG_W500+o.get("poster_path").getAsString() : "";
        String backdrop = o.has("backdrop_path") && !o.get("backdrop_path").isJsonNull()
            ? Constants.IMG_ORI+o.get("backdrop_path").getAsString() : poster;
        double vote = (o.has("vote_average") && !o.get("vote_average").isJsonNull()) ? o.get("vote_average").getAsDouble() : 0;
        return new MediaItem(id, title, poster, backdrop, vote, type, "tmdb");
    }
    private MediaItem fmtAnime(JsonObject raw) {
        JsonObject o = raw;
        if (raw.has("node") && raw.get("node").isJsonObject()) {
            o = raw.getAsJsonObject("node");
        }
        String id = "";
        if (o.has("mal_id") && !o.get("mal_id").isJsonNull()) id = String.valueOf(o.get("mal_id").getAsLong());
        else if (o.has("id") && !o.get("id").isJsonNull()) id = String.valueOf(o.get("id").getAsLong());

        String title = gs(o, "title_english", gs(o, "title", "Untitled"));
        if (title.isEmpty() && o.has("title")) title = gs(o, "title", "Untitled");

        String img = "";
        if (o.has("large_image_url") && !o.get("large_image_url").isJsonNull()) {
            img = o.get("large_image_url").getAsString();
        } else if (o.has("main_picture") && o.get("main_picture").isJsonObject()) {
            JsonObject mp = o.getAsJsonObject("main_picture");
            img = gs(mp, "large", gs(mp, "medium", ""));
        } else if (o.has("images") && o.get("images").isJsonObject()) {
            JsonObject j = o.get("images").getAsJsonObject();
            if (j.has("jpg") && j.get("jpg").isJsonObject()) img = gs(j.get("jpg").getAsJsonObject(), "large_image_url", "");
        }

        double score = 0;
        if (o.has("score") && !o.get("score").isJsonNull()) score = o.get("score").getAsDouble();
        else if (o.has("mean") && !o.get("mean").isJsonNull()) score = o.get("mean").getAsDouble();

        String type = gs(o, "type", gs(o, "media_type", "Anime"));
        if ("TV".equalsIgnoreCase(type)) type = "TV Show";
        else if ("Movie".equalsIgnoreCase(type)) type = "Movie";
        else type = "Anime";
        return new MediaItem(id, title, img, img, score, type, "jikan");
    }
    private String gs(JsonObject o, String k, String def) {
        return (o.has(k)&&!o.get(k).isJsonNull()) ? o.get(k).getAsString() : def;
    }
    private List<MediaItem> parseResults(JsonObject r, String type) {
        List<MediaItem> list = new ArrayList<MediaItem>();
        if (r!=null&&r.has("results")) for (JsonElement e:r.get("results").getAsJsonArray()) list.add(fmtTmdb(e.getAsJsonObject(),type));
        return list;
    }
    private List<MediaItem> parseAnime(JsonObject r) {
        List<MediaItem> list = new ArrayList<MediaItem>();
        if (r!=null&&r.has("data")) for (JsonElement e:r.get("data").getAsJsonArray()) list.add(fmtAnime(e.getAsJsonObject()));
        return list;
    }

    // ── Fetch methods ──────────────────────────────────────────────────────
    public void fetchTrendingMovies(final Callback<List<MediaItem>> cb) {
        fetchJson(tmdb("/trending/movie/day?page=1"), new Callback<JsonObject>() {
            public void onResult(JsonObject r) { cb.onResult(parseResults(r,"Movie")); }
            public void onError(Exception e) { cb.onResult(new ArrayList<MediaItem>()); }
        });
    }
    public void fetchTrendingTv(final Callback<List<MediaItem>> cb) {
        fetchJson(tmdb("/trending/tv/day?page=1"), new Callback<JsonObject>() {
            public void onResult(JsonObject r) { cb.onResult(parseResults(r,"TV Show")); }
            public void onError(Exception e) { cb.onResult(new ArrayList<MediaItem>()); }
        });
    }
    public void fetchPopularMovies(final Callback<List<MediaItem>> cb) {
        fetchJson(tmdb("/movie/popular?page=1"), new Callback<JsonObject>() {
            public void onResult(JsonObject r) { cb.onResult(parseResults(r,"Movie")); }
            public void onError(Exception e) { cb.onResult(new ArrayList<MediaItem>()); }
        });
    }
    public void fetchPopularTv(final Callback<List<MediaItem>> cb) {
        fetchJson(tmdb("/tv/popular?page=1"), new Callback<JsonObject>() {
            public void onResult(JsonObject r) { cb.onResult(parseResults(r,"TV Show")); }
            public void onError(Exception e) { cb.onResult(new ArrayList<MediaItem>()); }
        });
    }
    public void fetchTopRatedMovies(final Callback<List<MediaItem>> cb) {
        fetchJson(tmdb("/movie/top_rated?page=1"), new Callback<JsonObject>() {
            public void onResult(JsonObject r) { cb.onResult(parseResults(r,"Movie")); }
            public void onError(Exception e) { cb.onResult(new ArrayList<MediaItem>()); }
        });
    }
    public void fetchTopRatedTv(final Callback<List<MediaItem>> cb) {
        fetchJson(tmdb("/tv/top_rated?page=1"), new Callback<JsonObject>() {
            public void onResult(JsonObject r) { cb.onResult(parseResults(r,"TV Show")); }
            public void onError(Exception e) { cb.onResult(new ArrayList<MediaItem>()); }
        });
    }
    public void fetchLatestAnime(final Callback<List<MediaItem>> cb) {
        fetchJson(jikan("/seasons/now"), new Callback<JsonObject>() {
            public void onResult(JsonObject r) { cb.onResult(parseAnime(r)); }
            public void onError(Exception e) { cb.onResult(new ArrayList<MediaItem>()); }
        });
    }
    public void fetchPopularAnime(final Callback<List<MediaItem>> cb) {
        fetchJson(jikan("/anime?filter=bypopularity"), new Callback<JsonObject>() {
            public void onResult(JsonObject r) { 
                List<MediaItem> all = parseAnime(r);
                cb.onResult(all != null ? all : new ArrayList<MediaItem>()); 
            }
            public void onError(Exception e) {
                main.postDelayed(new Runnable() {
                    public void run() {
                        fetchJson(jikan("/anime?filter=bypopularity"), new Callback<JsonObject>() {
                            public void onResult(JsonObject r) { cb.onResult(parseAnime(r)); }
                            public void onError(Exception ex) { cb.onResult(new ArrayList<MediaItem>()); }
                        });
                    }
                }, 1200);
            }
        });
    }
    public void fetchTopRatedAnime(final Callback<List<MediaItem>> cb) {
        fetchJson(jikan("/top/anime"), new Callback<JsonObject>() {
            public void onResult(JsonObject r) { cb.onResult(parseAnime(r)); }
            public void onError(Exception e) { cb.onResult(new ArrayList<MediaItem>()); }
        });
    }
    private List<MediaItem> parseAnimeFiltered(JsonObject r, String requiredType) {
        List<MediaItem> list = new ArrayList<MediaItem>();
        if (r != null && r.has("data")) {
            for (com.google.gson.JsonElement e : r.get("data").getAsJsonArray()) {
                JsonObject o = e.getAsJsonObject();
                if (o.has("type") && !o.get("type").isJsonNull()) {
                    String type = o.get("type").getAsString();
                    if (requiredType == null || requiredType.equalsIgnoreCase(type)) {
                        list.add(fmtAnime(o));
                    }
                }
            }
        }
        return list;
    }

    public void fetchTopRatedAnimeMovies(final Callback<List<MediaItem>> cb) {
        fetchJson(jikan("/top/anime"), new Callback<JsonObject>() {
            public void onResult(JsonObject r) { cb.onResult(parseAnimeFiltered(r, "Movie")); }
            public void onError(Exception ex) { cb.onResult(new ArrayList<MediaItem>()); }
        });
    }
    public void fetchTopRatedAnimeSeries(final Callback<List<MediaItem>> cb) {
        fetchJson(jikan("/top/anime"), new Callback<JsonObject>() {
            public void onResult(JsonObject r) { cb.onResult(parseAnimeFiltered(r, "TV")); }
            public void onError(Exception ex) { cb.onResult(new ArrayList<MediaItem>()); }
        });
    }
    public void fetchSeasonalAnime(int year, String season, final Callback<List<MediaItem>> cb) {
        fetchJson(jikan("/seasons/" + year + "/" + season), new Callback<JsonObject>() {
            public void onResult(JsonObject r) { cb.onResult(parseAnime(r)); }
            public void onError(Exception e) { cb.onResult(new ArrayList<MediaItem>()); }
        });
    }
    public void fetchLatestAnimeMovies(final Callback<List<MediaItem>> cb) {
        fetchJson(jikan("/seasons/now"), new Callback<JsonObject>() {
            public void onResult(JsonObject r) { cb.onResult(parseAnimeFiltered(r, "Movie")); }
            public void onError(Exception e) { cb.onResult(new ArrayList<MediaItem>()); }
        });
    }
    public void fetchLatestAnimeSeries(final Callback<List<MediaItem>> cb) {
        fetchJson(jikan("/seasons/now"), new Callback<JsonObject>() {
            public void onResult(JsonObject r) { cb.onResult(parseAnimeFiltered(r, "TV")); }
            public void onError(Exception e) { cb.onResult(new ArrayList<MediaItem>()); }
        });
    }
    public void fetchMoviesByGenre(int gid, int page, final Callback<List<MediaItem>> cb) {
        fetchJson(tmdb("/discover/movie?with_genres="+gid+"&page="+page), new Callback<JsonObject>() {
            public void onResult(JsonObject r) { cb.onResult(parseResults(r,"Movie")); }
            public void onError(Exception e) { cb.onResult(new ArrayList<MediaItem>()); }
        });
    }
    public void fetchTvByGenre(int gid, int page, final Callback<List<MediaItem>> cb) {
        fetchJson(tmdb("/discover/tv?with_genres="+gid+"&page="+page), new Callback<JsonObject>() {
            public void onResult(JsonObject r) { cb.onResult(parseResults(r,"TV Show")); }
            public void onError(Exception e) { cb.onResult(new ArrayList<MediaItem>()); }
        });
    }
    private String getAnimeGenreParam(int gid) {
        switch (gid) {
            case 1: return "Action";
            case 2: return "Adventure";
            case 4: return "Comedy";
            case 8: return "Drama";
            case 10: return "Fantasy";
            case 14: return "Horror";
            case 7: return "Mystery";
            case 22: return "Romance";
            case 24: return "Sci-Fi";
            case 41: return "Thriller";
            default: return String.valueOf(gid);
        }
    }

    public void fetchAnimeByGenre(int gid, int page, final Callback<List<MediaItem>> cb) {
        final List<MediaItem> combined = new ArrayList<MediaItem>();
        final int[] pending = {2};
        final Callback<List<MediaItem>> merge = new Callback<List<MediaItem>>() {
            public void onResult(List<MediaItem> r) {
                if (r != null && !r.isEmpty()) combined.addAll(r);
                pending[0]--;
                if (pending[0] <= 0 || !combined.isEmpty()) cb.onResult(new ArrayList<MediaItem>(combined));
            }
            public void onError(Exception e) {
                pending[0]--;
                if (pending[0] <= 0 || !combined.isEmpty()) cb.onResult(new ArrayList<MediaItem>(combined));
            }
        };
        fetchJson(tmdb("/discover/tv?with_genres=16," + gid + "&with_original_language=ja&page=" + page), new Callback<JsonObject>() {
            public void onResult(JsonObject r) { merge.onResult(parseResults(r, "Anime")); }
            public void onError(Exception e) { merge.onError(e); }
        });
        fetchJson(jikan("/anime?genre=" + getAnimeGenreParam(gid)), new Callback<JsonObject>() {
            public void onResult(JsonObject r) { merge.onResult(parseAnime(r)); }
            public void onError(Exception e) { merge.onError(e); }
        });
    }

    public void search(String query, final Callback<List<MediaItem>> cb) {
        String enc; try { enc = java.net.URLEncoder.encode(query,"UTF-8"); } catch(Exception e){enc=query;}
        final String q = enc;
        final List<MediaItem> combined = new ArrayList<MediaItem>();
        final int[] pending = {2};
        final Callback<List<MediaItem>> merge = new Callback<List<MediaItem>>() {
            public void onResult(List<MediaItem> r) {
                if (r != null && !r.isEmpty()) combined.addAll(r);
                pending[0]--;
                cb.onResult(new ArrayList<MediaItem>(combined));
            }
            public void onError(Exception e) {
                pending[0]--;
                if (pending[0] <= 0 || !combined.isEmpty()) cb.onResult(new ArrayList<MediaItem>(combined));
            }
        };
        fetchJson(tmdb("/search/multi?query="+q), new Callback<JsonObject>() {
            public void onResult(JsonObject r) {
                List<MediaItem> list = new ArrayList<MediaItem>();
                if (r.has("results")) for (JsonElement el:r.get("results").getAsJsonArray()) {
                    JsonObject o = el.getAsJsonObject(); String mt = gs(o,"media_type","");
                    if ("movie".equals(mt)) list.add(fmtTmdb(o,"Movie"));
                    else if ("tv".equals(mt)) list.add(fmtTmdb(o,"TV Show"));
                }
                merge.onResult(list);
            }
            public void onError(Exception e) { merge.onError(e); }
        });
        fetchJson(jikan("/anime?q="+q), new Callback<JsonObject>() {
            public void onResult(JsonObject r) { merge.onResult(parseAnime(r)); }
            public void onError(Exception e) { merge.onError(e); }
        });
    }

    public void fetchDetails(final MediaItem item, final Callback<MediaDetails> cb) {
        if ("jikan".equals(item.getSource())) {
            fetchJson("https://arm.haglund.dev/api/v2/ids?source=myanimelist&id=" + item.getId(), new Callback<JsonObject>() {
                public void onResult(JsonObject r) {
                    if (r != null && r.has("themoviedb") && !r.get("themoviedb").isJsonNull()) {
                        String tmdbId = r.get("themoviedb").getAsString();
                        MediaItem tmdbItem = new MediaItem(tmdbId, item.getTitle(), item.getPosterPath(), item.getBackdropPath(), item.getVoteAverage(), item.getType(), "tmdb");
                        fetchDetailsFromTmdb(item, tmdbItem, cb);
                    } else if (r != null && r.has("imdb") && !r.get("imdb").isJsonNull()) {
                        String imdbId = r.get("imdb").getAsString();
                        fetchJson(tmdb("/find/" + imdbId + "?external_source=imdb_id"), new Callback<JsonObject>() {
                            public void onResult(JsonObject findResult) {
                                MediaItem tmdbItem = null;
                                if (findResult.has("tv_results") && findResult.get("tv_results").getAsJsonArray().size() > 0) {
                                    tmdbItem = fmtTmdb(findResult.get("tv_results").getAsJsonArray().get(0).getAsJsonObject(), "TV Show");
                                } else if (findResult.has("movie_results") && findResult.get("movie_results").getAsJsonArray().size() > 0) {
                                    tmdbItem = fmtTmdb(findResult.get("movie_results").getAsJsonArray().get(0).getAsJsonObject(), "Movie");
                                }
                                if (tmdbItem != null) fetchDetailsFromTmdb(item, tmdbItem, cb);
                                else fetchAnimeDetails(item, cb);
                            }
                            public void onError(Exception e) { fetchAnimeDetails(item, cb); }
                        });
                    } else fetchAnimeDetails(item, cb);
                }
                public void onError(Exception e) { fetchAnimeDetails(item, cb); }
            });
            return;
        }
        fetchDetailsFromTmdb(item, item, cb);
    }

    private void fetchDetailsFromTmdb(final MediaItem originalItem, final MediaItem fetchItem, final Callback<MediaDetails> cb) {
        final String type = "Movie".equals(fetchItem.getType()) ? "movie" : "tv";
        fetchJson(tmdb("/"+type+"/"+fetchItem.getId()), new Callback<JsonObject>() {
            public void onResult(JsonObject r) {
                final MediaDetails d = new MediaDetails();
                d.setId(fetchItem.getId()); d.setType(originalItem.getType()); d.setSource(fetchItem.getSource());
                d.setTitle(gs(r,"title",gs(r,"name",originalItem.getTitle())));
                d.setOverview(gs(r,"overview","No overview available."));
                d.setStatus(gs(r,"status","N/A")); d.setOriginalLanguage(gs(r,"original_language","en"));
                
                // Use originalItem (Jikan) posters if available, else TMDB
                String tmdbPoster = r.has("poster_path") && !r.get("poster_path").isJsonNull() ? Constants.IMG_W500+r.get("poster_path").getAsString() : null;
                String tmdbBackdrop = r.has("backdrop_path") && !r.get("backdrop_path").isJsonNull() ? Constants.IMG_ORI+r.get("backdrop_path").getAsString() : null;
                d.setPosterPath((originalItem.getPosterPath() != null && !originalItem.getPosterPath().isEmpty()) ? originalItem.getPosterPath() : (tmdbPoster != null ? tmdbPoster : ""));
                d.setBackdropPath((originalItem.getBackdropPath() != null && !originalItem.getBackdropPath().isEmpty()) ? originalItem.getBackdropPath() : (tmdbBackdrop != null ? tmdbBackdrop : d.getPosterPath()));

                d.setVoteAverage((r.has("vote_average") && !r.get("vote_average").isJsonNull()) ? r.get("vote_average").getAsDouble() : originalItem.getVoteAverage());
                if (r.has("genres")&&r.get("genres").isJsonArray()) { StringBuilder sb=new StringBuilder(); for(JsonElement g:r.get("genres").getAsJsonArray()){if(sb.length()>0)sb.append(", ");sb.append(g.getAsJsonObject().get("name").getAsString());} d.setGenres(sb.toString()); }
                if (r.has("runtime")&&!r.get("runtime").isJsonNull()) d.setRuntime(r.get("runtime").getAsInt());
                if (r.has("episode_run_time")&&r.get("episode_run_time").isJsonArray()){JsonArray a=r.get("episode_run_time").getAsJsonArray();if(a.size()>0)d.setRuntime(a.get(0).getAsInt());}
                if (r.has("release_date")) d.setReleaseDate(gs(r,"release_date","N/A"));
                if (r.has("first_air_date")) d.setReleaseDate(gs(r,"first_air_date","N/A"));
                if (r.has("seasons")&&r.get("seasons").isJsonArray()){List<Integer> ss=new ArrayList<Integer>();for(JsonElement s:r.get("seasons").getAsJsonArray()){JsonObject so=s.getAsJsonObject();int sn=so.has("season_number")?so.get("season_number").getAsInt():0;if(sn>0)ss.add(sn);}d.setAvailableSeasons(ss);}
                cb.onResult(d);
                fetchCredits(fetchItem,d,cb); fetchSimilar(fetchItem,d,cb); fetchExternalIds(fetchItem,d,cb);
            }
            public void onError(Exception e) { cb.onError(e); }
        });
    }

    private void fetchAnimeDetails(final MediaItem item, final Callback<MediaDetails> cb) {
        fetchJson(jikan("/anime/"+item.getId()), new Callback<JsonObject>() {
            public void onResult(JsonObject r) {
                JsonObject data = r.has("data") ? r.get("data").getAsJsonObject() : r;
                MediaDetails d = new MediaDetails();
                d.setId(item.getId()); d.setType("Anime"); d.setSource("jikan");
                String title = gs(data, "title_english", gs(data, "title", "Untitled"));
                if (data.has("alternative_titles") && data.get("alternative_titles").isJsonObject()) {
                    JsonObject alt = data.getAsJsonObject("alternative_titles");
                    if (alt.has("en") && !alt.get("en").isJsonNull() && !alt.get("en").getAsString().isEmpty()) {
                        title = alt.get("en").getAsString();
                    }
                }
                d.setTitle(title);
                String img = "";
                if (data.has("large_image_url") && !data.get("large_image_url").isJsonNull()) {
                    img = data.get("large_image_url").getAsString();
                } else if (data.has("main_picture") && data.get("main_picture").isJsonObject()) {
                    JsonObject mp = data.getAsJsonObject("main_picture");
                    img = gs(mp, "large", gs(mp, "medium", ""));
                } else if (data.has("images") && data.get("images").isJsonObject()) {
                    JsonObject imgs = data.get("images").getAsJsonObject();
                    if (imgs.has("jpg")) img = gs(imgs.get("jpg").getAsJsonObject(), "large_image_url", "");
                }
                d.setPosterPath(img.isEmpty() ? item.getPosterPath() : img);
                d.setBackdropPath(img.isEmpty() ? item.getBackdropPath() : img);
                d.setOverview(gs(data, "synopsis", "No synopsis available."));
                double score = 0;
                if (data.has("score") && !data.get("score").isJsonNull()) score = data.get("score").getAsDouble();
                else if (data.has("mean") && !data.get("mean").isJsonNull()) score = data.get("mean").getAsDouble();
                d.setVoteAverage(score);
                d.setStatus(gs(data, "status", "N/A"));
                if (data.has("genres") && data.get("genres").isJsonArray()) {
                    StringBuilder sb = new StringBuilder();
                    for (JsonElement g : data.get("genres").getAsJsonArray()) {
                        if (sb.length() > 0) sb.append(", ");
                        if (g.isJsonObject()) {
                            sb.append(g.getAsJsonObject().get("name").getAsString());
                        } else if (g.isJsonPrimitive()) {
                            sb.append(g.getAsString());
                        }
                    }
                    d.setGenres(sb.toString());
                }
                cb.onResult(d);
            }
            public void onError(Exception e) { cb.onError(e); }
        });
    }

    private void fetchCredits(final MediaItem item, final MediaDetails d, final Callback<MediaDetails> cb) {
        String type="Movie".equals(item.getType())?"movie":"tv";
        fetchJson(tmdb("/"+type+"/"+item.getId()+"/credits"), new Callback<JsonObject>() {
            public void onResult(JsonObject r) {
                if(r.has("cast")){List<CastMember> cast=new ArrayList<CastMember>();int cnt=0;for(JsonElement e:r.get("cast").getAsJsonArray()){if(cnt++>=12)break;JsonObject o=e.getAsJsonObject();CastMember cm=new CastMember();cm.setName(gs(o,"name",""));if(o.has("profile_path")&&!o.get("profile_path").isJsonNull())cm.setProfilePath(Constants.IMG_W500+o.get("profile_path").getAsString());cast.add(cm);}d.setCast(cast);cb.onResult(d);}
                if(r.has("crew")){for(JsonElement e:r.get("crew").getAsJsonArray()){JsonObject o=e.getAsJsonObject();if("Director".equals(gs(o,"job",""))){d.setDirector(gs(o,"name","N/A"));break;}}cb.onResult(d);}
            }
            public void onError(Exception e) {}
        });
    }

    private void fetchSimilar(final MediaItem item, final MediaDetails d, final Callback<MediaDetails> cb) {
        String type="Movie".equals(item.getType())?"movie":"tv";
        fetchJson(tmdb("/"+type+"/"+item.getId()+"/similar"), new Callback<JsonObject>() {
            public void onResult(JsonObject r) {
                if(r.has("results")){List<MediaItem> sim=new ArrayList<MediaItem>();int cnt=0;for(JsonElement e:r.get("results").getAsJsonArray()){if(cnt++>=12)break;sim.add(fmtTmdb(e.getAsJsonObject(),item.getType()));}d.setSimilar(sim);cb.onResult(d);}
            }
            public void onError(Exception e) {}
        });
    }

    private void fetchExternalIds(final MediaItem item, final MediaDetails d, final Callback<MediaDetails> cb) {
        final String type="Movie".equals(item.getType())?"movie":"tv";
        fetchJson(tmdb("/"+type+"/"+item.getId()+"/external_ids"), new Callback<JsonObject>() {
            public void onResult(JsonObject r) {
                if(r.has("imdb_id")&&!r.get("imdb_id").isJsonNull()){
                    final String imdbId = r.get("imdb_id").getAsString();
                    d.setImdbId(imdbId);
                    cb.onResult(d);
                    if("tv".equals(type) || "TV Show".equals(d.getType())) {
                        fetchJson("https://api.tiffara.com/titles/" + imdbId + "/seasons", new Callback<JsonObject>() {
                            public void onResult(JsonObject sr) {
                                if(sr.has("seasons") && sr.get("seasons").isJsonArray()) {
                                    List<Integer> ss = new ArrayList<Integer>();
                                    for(com.google.gson.JsonElement se : sr.get("seasons").getAsJsonArray()) {
                                        try { ss.add(Integer.parseInt(gs(se.getAsJsonObject(), "season", "0"))); } catch(Exception ignored) {}
                                    }
                                    if(ss.size() > 0) {
                                        d.setAvailableSeasons(ss);
                                        cb.onResult(d);
                                    }
                                }
                            }
                            public void onError(Exception e) {}
                        });
                    }
                }
            }
            public void onError(Exception e) {}
        });
    }

    public interface ArrayCallback { void onResult(JsonArray a); void onError(Exception e); }

    private void fetchJsonArray(final String url, final ArrayCallback cb) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    java.net.HttpURLConnection c=(java.net.HttpURLConnection)new java.net.URL(url).openConnection();
                    c.setRequestMethod("GET");
                    // handle 301 gracefully if tvmaze redirects
                    if (c.getResponseCode() == 301) {
                        String loc = c.getHeaderField("Location");
                        if (loc != null) {
                            c = (java.net.HttpURLConnection)new java.net.URL(loc).openConnection();
                            c.setRequestMethod("GET");
                        }
                    }
                    java.io.BufferedReader r=new java.io.BufferedReader(new java.io.InputStreamReader(c.getInputStream()));
                    StringBuilder b=new StringBuilder(); String l;
                    while((l=r.readLine())!=null) b.append(l);
                    r.close();
                    final JsonArray j=new com.google.gson.Gson().fromJson(b.toString(), JsonArray.class);
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable(){public void run(){cb.onResult(j);}});
                } catch(final Exception e) { new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable(){public void run(){cb.onError(e);}}); }
            }
        }).start();
    }

    public void fetchEpisodesByImdb(final String imdbId, final int season, final String posterUrl, final Callback<List<Episode>> cb) {
        fetchJson("https://api.tiffara.com/titles/" + imdbId + "/episodes?season=" + season, new Callback<JsonObject>() {
            public void onResult(JsonObject root) {
                List<Episode> eps = new ArrayList<Episode>();
                int totalCount = 0;
                try {
                    if (root != null && root.has("totalCount") && !root.get("totalCount").isJsonNull()) {
                        totalCount = root.get("totalCount").getAsInt();
                    }
                } catch (Exception ignored) {}
                if (root != null && root.has("episodes") && root.get("episodes").isJsonArray()) {
                    for (com.google.gson.JsonElement e : root.get("episodes").getAsJsonArray()) {
                        JsonObject o = e.getAsJsonObject();
                        Episode ep = new Episode();
                        ep.setEpisodeNumber(o.has("episodeNumber") && !o.get("episodeNumber").isJsonNull() ? o.get("episodeNumber").getAsInt() : 0);
                        ep.setSeasonNumber(season);
                        ep.setName(gs(o, "title", "Episode " + ep.getEpisodeNumber()));
                        if (o.has("releaseDate") && !o.get("releaseDate").isJsonNull()) {
                            JsonObject rd = o.get("releaseDate").getAsJsonObject();
                            int y = rd.has("year") ? rd.get("year").getAsInt() : 0;
                            int m = rd.has("month") ? rd.get("month").getAsInt() : 0;
                            int d = rd.has("day") ? rd.get("day").getAsInt() : 0;
                            if (y > 0) ep.setAirDate(y + "-" + (m < 10 ? "0" + m : m) + "-" + (d < 10 ? "0" + d : d));
                        }
                        ep.setPlot(gs(o, "plot", ""));
                        if (o.has("primaryImage") && !o.get("primaryImage").isJsonNull()) {
                            JsonObject img = o.get("primaryImage").getAsJsonObject();
                            ep.setStillPath(gs(img, "url", ""));
                        }
                        eps.add(ep);
                    }
                }
                if (totalCount > eps.size()) {
                    int lastEpNo = eps.size() > 0 ? eps.get(eps.size() - 1).getEpisodeNumber() : 0;
                    for (int i = eps.size(); i < totalCount; i++) {
                        Episode ep = new Episode();
                        lastEpNo++;
                        ep.setEpisodeNumber(lastEpNo);
                        ep.setSeasonNumber(season);
                        ep.setName("Episode " + lastEpNo);
                        ep.setAirDate("");
                        ep.setStillPath(posterUrl);
                        eps.add(ep);
                    }
                }
                cb.onResult(eps);
            }
            public void onError(Exception e) { cb.onResult(new ArrayList<Episode>()); }
        });
    }

    public void fetchEpisodes(String id, int season, final Callback<List<Episode>> cb) {
        fetchJson(tmdb("/tv/"+id+"/season/"+season), new Callback<JsonObject>() {
            public void onResult(JsonObject r) {
                List<Episode> eps=new ArrayList<Episode>();
                if(r.has("episodes")){for(JsonElement e:r.get("episodes").getAsJsonArray()){JsonObject o=e.getAsJsonObject();Episode ep=new Episode();ep.setEpisodeNumber(o.has("episode_number")?o.get("episode_number").getAsInt():0);ep.setSeasonNumber(season);ep.setName(gs(o,"name","Episode "+ep.getEpisodeNumber()));ep.setAirDate(gs(o,"air_date",""));ep.setPlot(gs(o,"overview",""));if(o.has("still_path")&&!o.get("still_path").isJsonNull())ep.setStillPath(Constants.IMG_W500+o.get("still_path").getAsString());eps.add(ep);}}
                cb.onResult(eps);
            }
            public void onError(Exception e) { cb.onResult(new ArrayList<Episode>()); }
        });
    }
}

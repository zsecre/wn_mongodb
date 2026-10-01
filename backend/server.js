require("dotenv").config();
const express = require("express");
const mongoose = require("mongoose");
const cors = require("cors");

const app = express();
const PORT = process.env.PORT || 5000;
const MONGODB_URI = process.env.MONGODB_URI || "mongodb://localhost:27017/watchninja";

app.use(cors());
app.use(express.json({ limit: "10mb" }));

// ── Schemas & Models ─────────────────────────────────────────────────────────

// Stats collection
const StatSchema = new mongoose.Schema({
  key: { type: String, required: true, unique: true, index: true },
  views: { type: Number, default: 0 },
  likes: { type: Number, default: 0 },
  dislikes: { type: Number, default: 0 },
  ratingSum: { type: Number, default: 0 },
  ratingCount: { type: Number, default: 0 },
  users: { type: Map, of: Object, default: {} }
}, { timestamps: true });

const Stat = mongoose.model("Stat", StatSchema);

// Comments collection
const CommentSchema = new mongoose.Schema({
  key: { type: String, required: true, index: true },
  commentId: { type: String, required: true, unique: true, index: true },
  parentId: { type: String, default: null, index: true },
  authorId: { type: String, required: true },
  authorName: { type: String, default: "User" },
  authorAvatar: { type: String, default: "" },
  timeAgo: { type: String, default: "" },
  text: { type: String, required: true },
  likeCount: { type: Number, default: 0 },
  replies: { type: Array, default: [] },
  createdAt: { type: Number, default: () => Date.now() }
}, { timestamps: true });

const Comment = mongoose.model("Comment", CommentSchema);

// Notifications collection
const NotificationSchema = new mongoose.Schema({
  id: { type: String, required: true, unique: true, index: true },
  userId: { type: String, required: true, index: true },
  type: { type: String, default: "like" },
  fromName: { type: String, default: "" },
  fromAvatar: { type: String, default: "" },
  msg: { type: String, default: "" },
  timestamp: { type: Number, default: () => Date.now(), index: true },
  read: { type: Boolean, default: false },
  mediaItem: { type: Object, default: {} },
  season: { type: Number, default: null },
  episode: { type: Number, default: null }
}, { timestamps: true });

const Notification = mongoose.model("Notification", NotificationSchema);

// Users collection
const UserSchema = new mongoose.Schema({
  userId: { type: String, required: true, unique: true, index: true },
  name: { type: String, default: "User" },
  avatar: { type: String, default: "" },
  lastActive: { type: Number, default: () => Date.now() }
}, { timestamps: true });

const User = mongoose.model("User", UserSchema);

// App Update collection
const AppUpdateSchema = new mongoose.Schema({
  versionCode: { type: Number, default: 4 },
  versionName: { type: String, default: "1.0.4" },
  title: { type: String, default: "WatchNinja Update Available" },
  changelog: { type: String, default: "Bug fixes and improvements" },
  apkUrl: { type: String, default: "" },
  size: { type: String, default: "7.5 MB" },
  forceUpdate: { type: Boolean, default: false }
}, { timestamps: true });

const AppUpdate = mongoose.model("AppUpdate", AppUpdateSchema);

// ── Connect to MongoDB ───────────────────────────────────────────────────────
mongoose.connect(MONGODB_URI, {
  serverSelectionTimeoutMS: 5000,
  autoIndex: true
}).then(() => {
  console.log("Connected to MongoDB successfully");
}).catch(err => {
  console.error("MongoDB connection error:", err.message);
});

// ── Health Check ─────────────────────────────────────────────────────────────
app.get("/health", (req, res) => {
  res.json({
    status: "ok",
    database: mongoose.connection.readyState === 1 ? "connected" : "disconnected",
    uptime: process.uptime()
  });
});

// ── Stats Routes ─────────────────────────────────────────────────────────────

// Single item stats
app.get("/api/stats/:key", async (req, res) => {
  try {
    const key = req.params.key;
    const stat = await Stat.findOne({ key });
    if (!stat) {
      return res.json({ key, views: 0, likes: 0, dislikes: 0, ratingSum: 0, ratingCount: 0, users: {} });
    }
    res.json(stat);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Batch stats for DetailsActivity (aggregates views, likes, dislikes, rating for multiple IDs without downloading the entire DB!)
app.post("/api/stats/batch", async (req, res) => {
  try {
    const targetIds = req.body.targetIds || [];
    if (!targetIds.length) {
      return res.json({ views: 0, likes: 0, dislikes: 0, ratingSum: 0, ratingCount: 0, ratingAverage: 0 });
    }

    // Build regex match conditions for each targetId
    // e.g. MOVIE_123, TV_123, ANIME_TV_123_S1, etc.
    const regexList = targetIds.map(tid => new RegExp(`(^|_)${tid}($|_)`));
    const stats = await Stat.find({
      $or: regexList.map(r => ({ key: r }))
    });

    let views = 0, likes = 0, dislikes = 0, ratingSum = 0, ratingCount = 0;
    for (const s of stats) {
      views += s.views || 0;
      likes += s.likes || 0;
      dislikes += s.dislikes || 0;
      ratingSum += s.ratingSum || 0;
      ratingCount += s.ratingCount || 0;
    }

    const ratingAverage = ratingCount > 0 ? (ratingSum / ratingCount) : 0;
    res.json({ views, likes, dislikes, ratingSum, ratingCount, ratingAverage });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Record user stat action (view, like, dislike, rating)
app.post("/api/stats/:key/action", async (req, res) => {
  try {
    const key = req.params.key;
    const { action, userId, rating, isLiked, isDisliked, userName } = req.body;

    let stat = await Stat.findOne({ key });
    if (!stat) {
      stat = new Stat({ key, views: 0, likes: 0, dislikes: 0, ratingSum: 0, ratingCount: 0, users: new Map() });
    }

    const usersMap = stat.users instanceof Map ? stat.users : new Map(Object.entries(stat.users || {}));
    const userObj = usersMap.get(userId) || {
      name: userName || "User",
      lastActive: Date.now(),
      viewed: false,
      liked: false,
      disliked: false,
      rating: 0
    };

    if (action === "view") {
      if (!userObj.viewed) {
        userObj.viewed = true;
        stat.views = (stat.views || 0) + 1;
      }
    } else if (action === "like_dislike") {
      const prevLiked = userObj.liked || false;
      const prevDisliked = userObj.disliked || false;

      if (isLiked && !prevLiked) {
        stat.likes = (stat.likes || 0) + 1;
        if (prevDisliked) stat.dislikes = Math.max(0, (stat.dislikes || 0) - 1);
      } else if (!isLiked && prevLiked) {
        stat.likes = Math.max(0, (stat.likes || 0) - 1);
      }

      if (isDisliked && !prevDisliked) {
        stat.dislikes = (stat.dislikes || 0) + 1;
        if (prevLiked) stat.likes = Math.max(0, (stat.likes || 0) - 1);
      } else if (!isDisliked && prevDisliked) {
        stat.dislikes = Math.max(0, (stat.dislikes || 0) - 1);
      }

      userObj.liked = !!isLiked;
      userObj.disliked = !!isDisliked;
    } else if (action === "rating" && rating !== undefined) {
      const prevRating = userObj.rating || 0;
      if (prevRating > 0) {
        stat.ratingSum = Math.max(0, (stat.ratingSum || 0) - prevRating + rating);
      } else {
        stat.ratingSum = (stat.ratingSum || 0) + rating;
        stat.ratingCount = (stat.ratingCount || 0) + 1;
      }
      userObj.rating = rating;
    }

    userObj.lastActive = Date.now();
    usersMap.set(userId, userObj);
    stat.users = usersMap;
    await stat.save();

    res.json(stat);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ── Comments Routes ──────────────────────────────────────────────────────────

// Fetch all comments and replies for a key
app.get("/api/comments/:key", async (req, res) => {
  try {
    const key = req.params.key;
    const comments = await Comment.find({ key, parentId: null }).sort({ createdAt: -1 });
    const replies = await Comment.find({ key, parentId: { $ne: null } }).sort({ createdAt: 1 });

    const replyMap = {};
    for (const r of replies) {
      if (!replyMap[r.parentId]) replyMap[r.parentId] = {};
      replyMap[r.parentId][r.commentId] = {
        authorId: r.authorId,
        authorName: r.authorName,
        authorAvatar: r.authorAvatar,
        timeAgo: r.timeAgo,
        text: r.text,
        likeCount: r.likeCount || 0
      };
    }

    const result = {};
    for (const c of comments) {
      result[c.commentId] = {
        authorId: c.authorId,
        authorName: c.authorName,
        authorAvatar: c.authorAvatar,
        timeAgo: c.timeAgo,
        text: c.text,
        likeCount: c.likeCount || 0,
        replies: replyMap[c.commentId] || {}
      };
    }

    res.json(result);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Add comment or reply
app.post("/api/comments/:key", async (req, res) => {
  try {
    const key = req.params.key;
    const { commentId, parentId, authorId, authorName, authorAvatar, timeAgo, text, likeCount } = req.body;
    const id = commentId || String(Date.now());

    const newComment = new Comment({
      key,
      commentId: id,
      parentId: parentId || null,
      authorId,
      authorName: authorName || "User",
      authorAvatar: authorAvatar || "",
      timeAgo: timeAgo || String(Date.now()),
      text,
      likeCount: likeCount || 0
    });

    await newComment.save();
    res.json({ success: true, comment: newComment });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Edit comment text or update likeCount
app.put("/api/comments/:key/:commentId", async (req, res) => {
  try {
    const { key, commentId } = req.params;
    const { text, likeCount } = req.body;
    const update = {};
    if (text !== undefined) update.text = text;
    if (likeCount !== undefined) update.likeCount = likeCount;

    const updated = await Comment.findOneAndUpdate(
      { key, commentId },
      { $set: update },
      { new: true }
    );
    res.json({ success: true, comment: updated });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Delete comment and its replies
app.delete("/api/comments/:key/:commentId", async (req, res) => {
  try {
    const { key, commentId } = req.params;
    await Comment.deleteOne({ key, commentId });
    await Comment.deleteMany({ key, parentId: commentId });
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ── Notifications Routes ─────────────────────────────────────────────────────

app.get("/api/notifications/:userId", async (req, res) => {
  try {
    const userId = req.params.userId;
    const notifications = await Notification.find({ userId }).sort({ timestamp: -1 }).limit(100);
    const result = {};
    for (const n of notifications) {
      result[n.id] = {
        id: n.id,
        type: n.type,
        fromName: n.fromName,
        fromAvatar: n.fromAvatar,
        msg: n.msg,
        timestamp: n.timestamp,
        read: n.read,
        mediaItem: n.mediaItem,
        season: n.season,
        episode: n.episode
      };
    }
    res.json(result);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post("/api/notifications/:userId", async (req, res) => {
  try {
    const userId = req.params.userId;
    const data = req.body;
    const notif = new Notification({
      id: data.id || String(Date.now()),
      userId,
      type: data.type || "like",
      fromName: data.fromName || "",
      fromAvatar: data.fromAvatar || "",
      msg: data.msg || "",
      timestamp: data.timestamp || Date.now(),
      read: false,
      mediaItem: data.mediaItem || {},
      season: data.season,
      episode: data.episode
    });
    await notif.save();
    res.json({ success: true, notification: notif });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.put("/api/notifications/:userId/read-all", async (req, res) => {
  try {
    const userId = req.params.userId;
    await Notification.updateMany({ userId }, { $set: { read: true } });
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.delete("/api/notifications/:userId", async (req, res) => {
  try {
    const userId = req.params.userId;
    await Notification.deleteMany({ userId });
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ── Users Routes ─────────────────────────────────────────────────────────────

app.put("/api/users/:userId", async (req, res) => {
  try {
    const userId = req.params.userId;
    const { name, avatar } = req.body;
    const update = { lastActive: Date.now() };
    if (name) update.name = name;
    if (avatar) update.avatar = avatar;

    const user = await User.findOneAndUpdate(
      { userId },
      { $set: update },
      { upsert: true, new: true }
    );
    res.json({ success: true, user });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ── App Update Routes ────────────────────────────────────────────────────────

app.get("/api/app_update", async (req, res) => {
  try {
    let update = await AppUpdate.findOne().sort({ createdAt: -1 });
    if (!update) {
      update = {
        versionCode: 4,
        versionName: "1.0.4",
        title: "WatchNinja Update Available",
        changelog: "• Fixed data loading and migrated database.",
        apkUrl: "https://github.com/zsecre/Watchable/releases/download/v1.0.4/app-release.6.apk",
        size: "7.5 MB",
        forceUpdate: false
      };
    }
    res.json(update);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ── Firebase Drop-In Compatibility Endpoints ─────────────────────────────────
// Allows seamless fallback / drop-in replacement if requests still end in .json
app.get("/stats.json", async (req, res) => {
  try {
    const all = await Stat.find();
    const result = {};
    for (const s of all) {
      result[s.key] = {
        views: s.views,
        likes: s.likes,
        dislikes: s.dislikes,
        ratingSum: s.ratingSum,
        ratingCount: s.ratingCount,
        users: s.users instanceof Map ? Object.fromEntries(s.users) : s.users
      };
    }
    res.json(result);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get("/stats/:key.json", (req, res) => {
  req.url = `/api/stats/${req.params.key}`;
  app.handle(req, res);
});

app.get("/comments/:key.json", (req, res) => {
  req.url = `/api/comments/${req.params.key}`;
  app.handle(req, res);
});

app.get("/notifications/:userId.json", (req, res) => {
  req.url = `/api/notifications/${req.params.userId}`;
  app.handle(req, res);
});

app.get("/app_update.json", (req, res) => {
  req.url = `/api/app_update`;
  app.handle(req, res);
});

app.listen(PORT, () => {
  console.log(`WatchNinja MongoDB API server listening on port ${PORT}`);
});

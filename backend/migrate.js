require("dotenv").config();
const mongoose = require("mongoose");

const rawBase = process.env.FIREBASE_DB_URL || "https://watchable-5a2ab-default-rtdb.asia-southeast1.firebasedatabase.app/";
const FIREBASE_BASE = rawBase.endsWith("/") ? rawBase : (rawBase + "/");
const MONGODB_URI = process.env.MONGODB_URI || "mongodb://localhost:27017/watchninja";

async function fetchJson(endpoint) {
  const url = FIREBASE_BASE + endpoint;
  console.log(`Fetching from Firebase: ${url}`);
  try {
    const res = await fetch(url);
    if (!res.ok) {
      console.warn(`Failed to fetch ${endpoint}: HTTP ${res.status}`);
      return null;
    }
    return await res.json();
  } catch (err) {
    console.error(`Error fetching ${endpoint}:`, err.message);
    return null;
  }
}

async function runMigration() {
  console.log("==================================================");
  console.log(" WatchNinja Firebase -> MongoDB Migration Tool");
  console.log("==================================================");
  console.log(`Source Firebase: ${FIREBASE_BASE}`);
  console.log(`Target MongoDB:  ${MONGODB_URI.replace(/:([^@]+)@/, ":****@")}`);
  console.log("Connecting to MongoDB...");

  await mongoose.connect(MONGODB_URI);
  console.log("Connected to MongoDB successfully!\n");

  const db = mongoose.connection.db;

  // 1. Migrate Stats
  console.log("--> Migrating Stats...");
  const rawStats = await fetchJson("stats.json");
  if (rawStats && typeof rawStats === "object") {
    const statsCol = db.collection("stats");
    let count = 0;
    for (const [key, val] of Object.entries(rawStats)) {
      if (!val || typeof val !== "object") continue;
      await statsCol.updateOne(
        { key },
        {
          $set: {
            key,
            views: Number(val.views || 0),
            likes: Number(val.likes || 0),
            dislikes: Number(val.dislikes || 0),
            ratingSum: Number(val.ratingSum || 0),
            ratingCount: Number(val.ratingCount || 0),
            users: val.users || {},
            updatedAt: new Date()
          }
        },
        { upsert: true }
      );
      count++;
    }
    console.log(`[DONE] Migrated ${count} stat entries.`);
  } else {
    console.log("[INFO] No stats found or failed to fetch.");
  }

  // 2. Migrate Comments
  console.log("\n--> Migrating Comments...");
  const rawComments = await fetchJson("comments.json");
  if (rawComments && typeof rawComments === "object") {
    const commentsCol = db.collection("comments");
    let count = 0;
    for (const [key, commentsGroup] of Object.entries(rawComments)) {
      if (!commentsGroup || typeof commentsGroup !== "object") continue;
      for (const [cId, cVal] of Object.entries(commentsGroup)) {
        if (!cVal || typeof cVal !== "object") continue;
        await commentsCol.updateOne(
          { key, commentId: cId },
          {
            $set: {
              key,
              commentId: cId,
              parentId: null,
              authorId: cVal.authorId || "",
              authorName: cVal.authorName || "User",
              authorAvatar: cVal.authorAvatar || "",
              timeAgo: cVal.timeAgo || "",
              text: cVal.text || "",
              likeCount: Number(cVal.likeCount || 0),
              createdAt: Number(cVal.createdAt || Date.now())
            }
          },
          { upsert: true }
        );
        count++;

        // Migrate replies if any
        if (cVal.replies && typeof cVal.replies === "object") {
          for (const [rId, rVal] of Object.entries(cVal.replies)) {
            if (!rVal || typeof rVal !== "object") continue;
            await commentsCol.updateOne(
              { key, commentId: rId },
              {
                $set: {
                  key,
                  commentId: rId,
                  parentId: cId,
                  authorId: rVal.authorId || "",
                  authorName: rVal.authorName || "User",
                  authorAvatar: rVal.authorAvatar || "",
                  timeAgo: rVal.timeAgo || "",
                  text: rVal.text || "",
                  likeCount: Number(rVal.likeCount || 0),
                  createdAt: Number(rVal.createdAt || Date.now())
                }
              },
              { upsert: true }
            );
            count++;
          }
        }
      }
    }
    console.log(`[DONE] Migrated ${count} comments and replies.`);
  } else {
    console.log("[INFO] No comments found or failed to fetch.");
  }

  // 3. Migrate Notifications
  console.log("\n--> Migrating Notifications...");
  const rawNotifs = await fetchJson("notifications.json");
  if (rawNotifs && typeof rawNotifs === "object") {
    const notifsCol = db.collection("notifications");
    let count = 0;
    for (const [userId, userNotifs] of Object.entries(rawNotifs)) {
      if (!userNotifs || typeof userNotifs !== "object") continue;
      for (const [nId, nVal] of Object.entries(userNotifs)) {
        if (!nVal || typeof nVal !== "object") continue;
        await notifsCol.updateOne(
          { id: nId },
          {
            $set: {
              id: nId,
              userId,
              type: nVal.type || "like",
              fromName: nVal.fromName || "",
              fromAvatar: nVal.fromAvatar || "",
              msg: nVal.msg || "",
              timestamp: Number(nVal.timestamp || Date.now()),
              read: Boolean(nVal.read),
              mediaItem: nVal.mediaItem || {},
              season: nVal.season || null,
              episode: nVal.episode || null
            }
          },
          { upsert: true }
        );
        count++;
      }
    }
    console.log(`[DONE] Migrated ${count} notifications.`);
  } else {
    console.log("[INFO] No notifications found or failed to fetch.");
  }

  // 4. Migrate Users
  console.log("\n--> Migrating Users...");
  const rawUsers = await fetchJson("users.json");
  if (rawUsers && typeof rawUsers === "object") {
    const usersCol = db.collection("users");
    let count = 0;
    for (const [userId, uVal] of Object.entries(rawUsers)) {
      if (!uVal || typeof uVal !== "object") continue;
      await usersCol.updateOne(
        { userId },
        {
          $set: {
            userId,
            name: uVal.name || "User",
            avatar: uVal.avatar || "",
            lastActive: Number(uVal.lastActive || Date.now())
          }
        },
        { upsert: true }
      );
      count++;
    }
    console.log(`[DONE] Migrated ${count} users.`);
  } else {
    console.log("[INFO] No users found or failed to fetch.");
  }

  // 5. Migrate App Update
  console.log("\n--> Migrating App Update Config...");
  const rawUpdate = await fetchJson("app_update.json");
  if (rawUpdate && typeof rawUpdate === "object") {
    const updateCol = db.collection("appupdates");
    await updateCol.updateOne(
      {},
      {
        $set: {
          versionCode: Number(rawUpdate.versionCode || 4),
          versionName: rawUpdate.versionName || "1.0.4",
          title: rawUpdate.title || "WatchNinja Update Available",
          changelog: rawUpdate.changelog || "Bug fixes and improvements",
          apkUrl: rawUpdate.apkUrl || "",
          size: rawUpdate.size || "7.5 MB",
          forceUpdate: Boolean(rawUpdate.forceUpdate)
        }
      },
      { upsert: true }
    );
    console.log("[DONE] Migrated app update configuration.");
  }

  console.log("\n==================================================");
  console.log(" Migration Complete! MongoDB is fully populated.");
  console.log("==================================================");
  await mongoose.disconnect();
  process.exit(0);
}

runMigration().catch(err => {
  console.error("Migration failed:", err);
  process.exit(1);
});

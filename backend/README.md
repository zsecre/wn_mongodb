# WatchNinja MongoDB REST API Backend

This is the dedicated backend service for the WatchNinja Android application, replacing Firebase Realtime Database with **MongoDB** (e.g. MongoDB Atlas).

## Why MongoDB?
- **No 10 GB/month limit**: MongoDB Atlas M0 (free) has 512 MB of storage and no restrictive 10GB monthly download cap.
- **Optimized Batch Requests**: Aggregates movie/show stats on the server instead of downloading the entire database to the client.
- **Security**: Database credentials stay securely on the server; the Android app only interacts with clean HTTPS endpoints.

## Environment Variables
Create a `.env` file in this folder:
```env
PORT=5000
MONGODB_URI=mongodb+srv://<username>:<password>@cluster0.xxxxx.mongodb.net/watchninja?retryWrites=true&w=majority
FIREBASE_DB_URL=https://watchable-5a2ab-default-rtdb.asia-southeast1.firebasedatabase.app/
```

## Step 1: Migrate Existing Firebase Data to MongoDB
Run:
```bash
npm install
npm run migrate
```
This transfers all your current stats, comments, notifications, users, and app update configuration directly from Firebase to your MongoDB Atlas cluster!

## Step 2: Run the Server Locally
```bash
npm start
```
The server will run on `http://localhost:5000`.

## Step 3: Free 1-Click Cloud Deployment
Deploy to **Render** or **Railway** for free in 2 minutes:
1. Push this folder to a GitHub repository.
2. In [Render.com](https://render.com), click **New Web Service** and connect your repository.
3. Set the Environment Variable:
   - `MONGODB_URI`: your MongoDB Atlas connection string.
4. Render gives you an HTTPS URL: `https://your-watchninja-api.onrender.com`.
5. Enter this URL into `Constants.java` (`DATABASE_API_URL`) or in `.env` in the Android app!

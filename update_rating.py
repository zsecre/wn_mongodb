with open(".build-outputs/java/com/watchable/MediaItem.java", "r") as f:
    content = f.read()

target = 'public String getRatingText() { return String.format("★ %.1f", voteAverage); }'
replacement = 'public String getRatingText() { return voteAverage == 0.0 ? "★ N/A" : String.format("★ %.1f", voteAverage); }'
content = content.replace(target, replacement)

with open(".build-outputs/java/com/watchable/MediaItem.java", "w") as f:
    f.write(content)
print("Done")

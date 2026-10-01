with open("app/src/main/java/com/watchable/PlayerActivity.java", "r") as f:
    content = f.read()

target1 = 'statImdbRating.setText(String.format("IMDB Ratings: %.1f", item.getVoteAverage()));'
rep1 = 'statImdbRating.setText(item.getVoteAverage() > 0 ? String.format("IMDB Ratings: %.1f", item.getVoteAverage()) : "IMDB Ratings: N/A");'
content = content.replace(target1, rep1)

target2 = 'statImdbRating.setText(String.format("IMDB Ratings: %.1f", aggregateRating));'
rep2 = 'statImdbRating.setText(aggregateRating > 0 ? String.format("IMDB Ratings: %.1f", aggregateRating) : "IMDB Ratings: N/A");'
content = content.replace(target2, rep2)

target3 = 'if (statRating != null) statRating.setText(String.format("Watchable Ratings: %.1f", avgRating));'
rep3 = 'if (statRating != null) statRating.setText(avgRating > 0 ? String.format("Watchable Ratings: %.1f", avgRating) : "Watchable Ratings: N/A");'
content = content.replace(target3, rep3)

with open("app/src/main/java/com/watchable/PlayerActivity.java", "w") as f:
    f.write(content)
print("Done")

package com.watchable;

import java.util.ArrayList;
import java.util.List;

public class Comment {
    private String id;
    private String authorId;
    private String authorName;
    private String authorAvatar;
    private String timeAgo;
    private String text;
    private int likeCount;
    private boolean isLiked;
    private boolean isDisliked;
    private List<Comment> replies;
    private String parentId;

    public Comment(String id, String authorId, String authorName, String authorAvatar, String timeAgo, String text, int likeCount) {
        this.id = id;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorAvatar = authorAvatar;
        this.timeAgo = timeAgo;
        this.text = text;
        this.likeCount = likeCount;
        this.replies = new ArrayList<>();
    }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    
    private boolean isRepliesExpanded;

    public boolean isRepliesExpanded() { return isRepliesExpanded; }
    public void setRepliesExpanded(boolean expanded) { this.isRepliesExpanded = expanded; }
    
    public String getId() { return id; }
    public String getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public String getAuthorAvatar() { return authorAvatar; }
    public String getTimeAgo() { return timeAgo; }
    public String getText() { return text; }
    public int getLikeCount() { return likeCount; }
    public boolean isLiked() { return isLiked; }
    public boolean isDisliked() { return isDisliked; }
    public List<Comment> getReplies() { return replies; }

    public void setLikeCount(int count) { this.likeCount = count; }
    public void setLiked(boolean liked) { this.isLiked = liked; }
    public void setDisliked(boolean disliked) { this.isDisliked = disliked; }
    public void addReply(Comment reply) { this.replies.add(reply); }
    public void removeReply(Comment reply) { this.replies.remove(reply); }
    public void setText(String text) { this.text = text; }
}

package com.watchable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.watchable.R;
import java.util.ArrayList;
import java.util.List;

import com.bumptech.glide.Glide;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> comments = new ArrayList<>();
    private OnCommentActionListener listener;
    private String currentUid;

    public interface OnCommentActionListener {
        void onReply(Comment comment);
        void onLike(Comment comment, boolean isLikeAction);
        void onEdit(Comment comment, boolean isReply, Comment parentComment);
        void onDelete(Comment comment, boolean isReply, Comment parentComment);
    }

    public CommentAdapter(String currentUid, OnCommentActionListener listener) {
        this.currentUid = currentUid;
        this.listener = listener;
    }

    private void loadAvatar(View itemView, String avatarUrl, String username, ImageView imageView, TextView textView) {
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            imageView.setVisibility(View.VISIBLE);
            textView.setVisibility(View.GONE);
            Glide.with(itemView.getContext()).load(avatarUrl).circleCrop().into(imageView);
        } else {
            imageView.setVisibility(View.GONE);
            textView.setVisibility(View.VISIBLE);
            if (username != null && !username.isEmpty()) {
                textView.setText(username.substring(0, 1).toUpperCase());
            } else {
                textView.setText("U");
            }
        }
    }

    private void showPopupMenu(View view, Comment comment, boolean isReply, Comment parentComment) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(view.getContext(), view);
        popup.getMenu().add(0, 1, 0, "Edit");
        popup.getMenu().add(0, 2, 0, "Delete");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                if (listener != null) listener.onEdit(comment, isReply, parentComment);
                return true;
            } else if (item.getItemId() == 2) {
                new android.app.AlertDialog.Builder(view.getContext())
                    .setTitle("Delete Comment")
                    .setMessage("Are you sure you want to delete this comment?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        if (listener != null) listener.onDelete(comment, isReply, parentComment);
                        if (isReply && parentComment != null) {
                            parentComment.removeReply(comment);
                            int parentPos = comments.indexOf(parentComment);
                            if (parentPos != -1) {
                                notifyItemChanged(parentPos);
                            }
                        } else {
                            int pos = comments.indexOf(comment);
                            if (pos != -1) {
                                comments.remove(pos);
                                notifyItemRemoved(pos);
                            }
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
                return true;
            }
            return false;
        });
        popup.show();
    }

    public void setComments(List<Comment> items) {
        this.comments = items;
        notifyDataSetChanged();
    }
    
    public void addComment(Comment comment) {
        this.comments.add(0, comment);
        notifyItemInserted(0);
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.bind(comment);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView authorName, timeAgo, text, likeCount, btnReply, btnToggleReplies;
        ImageButton btnLike, btnDislike, btnMore;
        LinearLayout repliesContainer;
        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            authorName = itemView.findViewById(R.id.comment_author);
            timeAgo = itemView.findViewById(R.id.comment_time);
            text = itemView.findViewById(R.id.comment_text);
            likeCount = itemView.findViewById(R.id.like_count);
            btnReply = itemView.findViewById(R.id.btn_reply);
            btnMore = itemView.findViewById(R.id.btn_more);
            btnLike = itemView.findViewById(R.id.btn_like);
            btnDislike = itemView.findViewById(R.id.btn_dislike);
            repliesContainer = itemView.findViewById(R.id.replies_container);
            btnToggleReplies = itemView.findViewById(R.id.btn_toggle_replies);
        }

        public void bind(final Comment comment) {
            String displayedName = comment.getAuthorName();
            String avatarUrl = comment.getAuthorAvatar();
            
            if (currentUid != null && currentUid.equals(comment.getAuthorId())) {
                String myName = AppPreferences.get(itemView.getContext()).getUserName();
                if (myName != null && !myName.isEmpty()) displayedName = myName;
                avatarUrl = AppPreferences.get(itemView.getContext()).getUserAvatar();
            }
            authorName.setText(displayedName);
            
            ImageView avatarImg = itemView.findViewById(R.id.comment_avatar);
            TextView avatarText = itemView.findViewById(R.id.comment_avatar_text);
            loadAvatar(itemView, avatarUrl, displayedName, avatarImg, avatarText);
            
            try {
                long time = Long.parseLong(comment.getId());
                timeAgo.setText(android.text.format.DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS));
            } catch(Exception e) {
                timeAgo.setText(comment.getTimeAgo());
            }
            
            text.setText(comment.getText());
            updateLikeUI(comment);

            if (currentUid != null && currentUid.equals(comment.getAuthorId())) {
                btnMore.setVisibility(View.VISIBLE);
                btnMore.setOnClickListener(v -> showPopupMenu(v, comment, false, null));
            } else {
                btnMore.setVisibility(View.GONE);
            }

            btnLike.setOnClickListener(v -> {
                comment.setLiked(!comment.isLiked());
                if(comment.isLiked() && comment.isDisliked()) {
                    comment.setDisliked(false);
                }
                comment.setLikeCount(comment.getLikeCount() + (comment.isLiked() ? 1 : -1));
                updateLikeUI(comment);
                if(listener != null) listener.onLike(comment, true);
            });

            btnDislike.setOnClickListener(v -> {
                comment.setDisliked(!comment.isDisliked());
                if(comment.isDisliked() && comment.isLiked()) {
                    comment.setLiked(false);
                    comment.setLikeCount(comment.getLikeCount() - 1);
                }
                updateLikeUI(comment);
                if(listener != null) listener.onLike(comment, false);
            });

            btnReply.setOnClickListener(v -> {
                if(listener != null) listener.onReply(comment);
            });

            if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
                btnToggleReplies.setVisibility(View.VISIBLE);
                btnToggleReplies.setText("View " + comment.getReplies().size() + (comment.getReplies().size() == 1 ? " reply" : " replies"));
                
                btnToggleReplies.setOnClickListener(v -> {
                    boolean newExpandedState = !comment.isRepliesExpanded();
                    comment.setRepliesExpanded(newExpandedState);
                    if (newExpandedState) {
                        repliesContainer.setVisibility(View.VISIBLE);
                        btnToggleReplies.setText("Hide replies");
                    } else {
                        repliesContainer.setVisibility(View.GONE);
                        btnToggleReplies.setText("View " + comment.getReplies().size() + (comment.getReplies().size() == 1 ? " reply" : " replies"));
                    }
                });
                
                if (comment.isRepliesExpanded()) {
                    repliesContainer.setVisibility(View.VISIBLE);
                    btnToggleReplies.setText("Hide replies");
                } else {
                    repliesContainer.setVisibility(View.GONE);
                }

                int numReplies = comment.getReplies().size();
                while (repliesContainer.getChildCount() < numReplies) {
                    View replyView = LayoutInflater.from(itemView.getContext()).inflate(R.layout.item_comment, repliesContainer, false);
                    repliesContainer.addView(replyView);
                }
                while (repliesContainer.getChildCount() > numReplies) {
                    repliesContainer.removeViewAt(repliesContainer.getChildCount() - 1);
                }

                for (int i = 0; i < numReplies; i++) {
                    Comment reply = comment.getReplies().get(i);
                    View replyView = repliesContainer.getChildAt(i);
                    replyView.setPadding(32, 8, 0, 8);
                    TextView rAuthor = replyView.findViewById(R.id.comment_author);
                    TextView rTime = replyView.findViewById(R.id.comment_time);
                    TextView rText = replyView.findViewById(R.id.comment_text);
                    TextView rLikeCount = replyView.findViewById(R.id.like_count);
                    View rReplyBtn = replyView.findViewById(R.id.btn_reply);
                    
                    String rDisplayedName = reply.getAuthorName();
                    String rAvatarUrl = reply.getAuthorAvatar();
                    
                    if (currentUid != null && currentUid.equals(reply.getAuthorId())) {
                        String myName = AppPreferences.get(itemView.getContext()).getUserName();
                        if (myName != null && !myName.isEmpty()) rDisplayedName = myName;
                        rAvatarUrl = AppPreferences.get(itemView.getContext()).getUserAvatar();
                    }
                    rAuthor.setText(rDisplayedName);
                    
                    ImageView rAvatarImg = replyView.findViewById(R.id.comment_avatar);
                    TextView rAvatarText = replyView.findViewById(R.id.comment_avatar_text);
                    loadAvatar(replyView, rAvatarUrl, rDisplayedName, rAvatarImg, rAvatarText);
                    rTime.setText(reply.getTimeAgo());
                    rText.setText(reply.getText());
                    rLikeCount.setText(String.valueOf(reply.getLikeCount()));
                    rReplyBtn.setVisibility(View.GONE);
                    replyView.findViewById(R.id.btn_toggle_replies).setVisibility(View.GONE);
                    
                    final ImageButton rBtnLike = replyView.findViewById(R.id.btn_like);
                    final ImageButton rBtnDislike = replyView.findViewById(R.id.btn_dislike);
                    final ImageButton rBtnMore = replyView.findViewById(R.id.btn_more);
                    
                    try {
                        long time = Long.parseLong(reply.getId());
                        rTime.setText(android.text.format.DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS));
                    } catch(Exception e) {
                        rTime.setText(reply.getTimeAgo());
                    }
                    
                    if (currentUid != null && currentUid.equals(reply.getAuthorId())) {
                        rBtnMore.setVisibility(View.VISIBLE);
                        rBtnMore.setOnClickListener(v -> showPopupMenu(v, reply, true, comment));
                    } else {
                        rBtnMore.setVisibility(View.GONE);
                    }
                    
                    Runnable updateReplyLikeUI = () -> {
                        rLikeCount.setText(reply.getLikeCount() > 0 ? String.valueOf(reply.getLikeCount()) : "");
                        if (reply.isLiked()) {
                            rBtnLike.setImageResource(R.drawable.ic_thumb_up);
                            rBtnLike.setColorFilter(androidx.core.content.ContextCompat.getColor(rBtnLike.getContext(), R.color.brand_cyan));
                        } else {
                            rBtnLike.setImageResource(R.drawable.ic_thumb_up_outline);
                            rBtnLike.setColorFilter(androidx.core.content.ContextCompat.getColor(rBtnLike.getContext(), R.color.text_muted));
                        }
                        if (reply.isDisliked()) {
                            rBtnDislike.setImageResource(R.drawable.ic_thumb_down);
                            rBtnDislike.setColorFilter(androidx.core.content.ContextCompat.getColor(rBtnDislike.getContext(), R.color.brand_cyan));
                        } else {
                            rBtnDislike.setImageResource(R.drawable.ic_thumb_down_outline);
                            rBtnDislike.setColorFilter(androidx.core.content.ContextCompat.getColor(rBtnDislike.getContext(), R.color.text_muted));
                        }
                    };

                    updateReplyLikeUI.run();

                    rBtnLike.setOnClickListener(v -> {
                        reply.setLiked(!reply.isLiked());
                        if (reply.isLiked() && reply.isDisliked()) {
                            reply.setDisliked(false);
                        }
                        reply.setLikeCount(reply.getLikeCount() + (reply.isLiked() ? 1 : -1));
                        updateReplyLikeUI.run();
                        if (listener != null) listener.onLike(reply, true);
                    });

                    rBtnDislike.setOnClickListener(v -> {
                        reply.setDisliked(!reply.isDisliked());
                        if (reply.isDisliked() && reply.isLiked()) {
                            reply.setLiked(false);
                            reply.setLikeCount(reply.getLikeCount() - 1);
                        }
                        updateReplyLikeUI.run();
                        if (listener != null) listener.onLike(reply, false);
                    });
                    }
            } else {
                repliesContainer.removeAllViews();
                repliesContainer.setVisibility(View.GONE);
                btnToggleReplies.setVisibility(View.GONE);
            }
        }
        
        private void updateLikeUI(Comment comment) {
            String lc = String.valueOf(comment.getLikeCount());
            likeCount.setText(comment.getLikeCount() > 0 ? lc : "");
            
            if (comment.isLiked()) {
                btnLike.setImageResource(R.drawable.ic_thumb_up);
                btnLike.setColorFilter(androidx.core.content.ContextCompat.getColor(btnLike.getContext(), R.color.brand_cyan));
            } else {
                btnLike.setImageResource(R.drawable.ic_thumb_up_outline);
                btnLike.setColorFilter(androidx.core.content.ContextCompat.getColor(btnLike.getContext(), R.color.text_muted));
            }
            
            if (comment.isDisliked()) {
                btnDislike.setImageResource(R.drawable.ic_thumb_down);
                btnDislike.setColorFilter(androidx.core.content.ContextCompat.getColor(btnDislike.getContext(), R.color.brand_cyan));
            } else {
                btnDislike.setImageResource(R.drawable.ic_thumb_down_outline);
                btnDislike.setColorFilter(androidx.core.content.ContextCompat.getColor(btnDislike.getContext(), R.color.text_muted));
            }
        }
    }
}

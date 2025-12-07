package com.kakaotechbootcamp.community.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "post_stat")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostStat {

    @Id
    @Column(name = "post_id")
    private Integer id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;

    @Column(name = "comment_count", nullable = false)
    private int commentCount = 0;

    public PostStat(Post post) {
        this.post = post;
    }

    public PostStat(Post post, int likeCount, int commentCount) {
        this.post = post;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
    }

    
    public void incrementViewCount() {
        viewCount++;
    }

    public void incrementLikeCount() {
        likeCount++;
    }

    public void decrementLikeCount() {
        likeCount = likeCount > 0 ? likeCount - 1 : 0;
    }

    public void incrementCommentCount() {
        commentCount++;
    }

    public void decrementCommentCount() {
        commentCount = commentCount > 0 ? commentCount - 1 : 0;
    }

    // 통계값 동기화용 메서드
    public void syncLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public void syncCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public void syncViewCount(int viewCount) {
        this.viewCount = viewCount;
    }
}

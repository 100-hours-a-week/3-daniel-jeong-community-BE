package com.kakaotechbootcamp.community.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "post_like")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike {

    @EmbeddedId
    private PostLikeId id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PostLike(Integer userId, Integer postId) {
        this.id = new PostLikeId(userId, postId);
    }
}

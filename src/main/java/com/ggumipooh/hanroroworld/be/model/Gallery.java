package com.ggumipooh.hanroroworld.be.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "galleries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gallery extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "gallery", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<GalleryImage> images = new ArrayList<>();

    @Builder.Default
    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Builder.Default
    @Column(name = "like_count")
    private Integer likeCount = 0;

    public void addImage(GalleryImage image) {
        images.add(image);
        image.setGallery(this);
    }

    public void removeImage(GalleryImage image) {
        images.remove(image);
        image.setGallery(null);
    }

    public Integer getViewCount() {
        return this.viewCount != null ? this.viewCount : 0;
    }

    public Integer getLikeCount() {
        return this.likeCount != null ? this.likeCount : 0;
    }

    public void incrementViewCount() {
        this.viewCount = getViewCount() + 1;
    }

    public void incrementLikeCount() {
        this.likeCount = getLikeCount() + 1;
    }

    public void decrementLikeCount() {
        int current = getLikeCount();
        if (current > 0) {
            this.likeCount = current - 1;
        }
    }

    public String getAuthorName() {
        if (user == null) return "Unknown";
        return user.getNickname() != null ? user.getNickname() : user.getName();
    }
}

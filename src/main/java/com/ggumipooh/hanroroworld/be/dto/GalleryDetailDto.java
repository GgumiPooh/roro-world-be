package com.ggumipooh.hanroroworld.be.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class GalleryDetailDto {
    private Long id;
    private String title;
    private String description;
    private String authorName;
    private List<String> imageUrls;
    private Integer likeCount;
    private Integer viewCount;
    private Integer commentCount;
    private Boolean isLikedByMe;
    private List<GalleryCommentDto> comments;
    private LocalDateTime createdAt;
}


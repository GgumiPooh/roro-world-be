package com.ggumipooh.hanroroworld.be.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class GalleryDto {
    private Long id;
    private String title;
    private String description;
    private String authorName;
    private Long authorId;
    private List<String> imageUrls;
    private Integer likeCount;
    private Integer viewCount;
    private Integer commentCount;
    private LocalDateTime createdAt;
}


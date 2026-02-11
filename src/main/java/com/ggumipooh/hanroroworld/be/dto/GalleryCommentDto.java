package com.ggumipooh.hanroroworld.be.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class GalleryCommentDto {
    private Long id;
    private String content;
    private String authorName;
    private LocalDateTime createdAt;
}


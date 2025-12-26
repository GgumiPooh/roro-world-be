package com.ggumipooh.hanroroworld.be.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class CommentDto {
    private Long id;
    private String content;
    private String author;
    private String createdAt;
}


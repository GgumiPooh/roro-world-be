package com.ggumipooh.hanroroworld.be.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MessageDto {
    private Long id;
    private String message;
    private String author;
    private String createdAt;
}


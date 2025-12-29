package com.ggumipooh.hanroroworld.be.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GalleryCreateRequest {
    private String title;
    private String description;
    private List<String> imageUrls; // Base64 encoded images or URLs
}


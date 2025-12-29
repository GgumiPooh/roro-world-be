package com.ggumipooh.hanroroworld.be.controller;

import com.ggumipooh.hanroroworld.be.dto.*;
import com.ggumipooh.hanroroworld.be.dto.mapper.GalleryMapper;
import com.ggumipooh.hanroroworld.be.model.Gallery;
import com.ggumipooh.hanroroworld.be.model.GalleryComment;
import com.ggumipooh.hanroroworld.be.service.GalleryService;
import com.ggumipooh.hanroroworld.be.service.TokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/gallery")
public class GalleryController {

    private final GalleryService galleryService;
    private final TokenService tokenService;

    @GetMapping
    public Page<GalleryDto> getGalleries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return galleryService.getGalleries(page, size)
                .map(gallery -> GalleryMapper.toDto(gallery,
                        (int) galleryService.getCommentCount(gallery.getId())));
    }

    @GetMapping("/search")
    public Page<GalleryDto> searchGalleries(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return galleryService.searchGalleries(keyword, page, size)
                .map(gallery -> GalleryMapper.toDto(gallery,
                        (int) galleryService.getCommentCount(gallery.getId())));
    }

    @GetMapping("/{id}")
    public GalleryDetailDto getGallery(@PathVariable Long id, HttpServletRequest request) {
        galleryService.incrementViewCount(id);

        Long userId = extractUserId(request);

        return galleryService.getById(id)
                .map(gallery -> {
                    List<GalleryComment> comments = galleryService.getComments(id);
                    boolean isLikedByMe = galleryService.isLikedByUser(id, userId);
                    return GalleryMapper.toDetailDto(gallery, comments, isLikedByMe);
                })
                .orElse(null);
    }

    // ===== 좋아요 =====

    @PostMapping("/{id}/like")
    public Object toggleLike(@PathVariable Long id, HttpServletRequest request, HttpServletResponse response) {
        Long userId = extractUserId(request);
        if (userId == null) {
            response.setStatus(401);
            return Map.of("error", "login_required");
        }

        try {
            boolean liked = galleryService.toggleLike(id, userId);
            Gallery gallery = galleryService.getById(id).orElse(null);
            return Map.of(
                    "liked", liked,
                    "likeCount", gallery != null ? gallery.getLikeCount() : 0);
        } catch (Exception ex) {
            response.setStatus(500);
            return Map.of("error", ex.getMessage());
        }
    }

    // ===== 댓글 =====

    @GetMapping("/{id}/comments")
    public List<GalleryCommentDto> getComments(@PathVariable Long id) {
        return galleryService.getComments(id).stream()
                .map(GalleryMapper::toCommentDto)
                .toList();
    }

    @PostMapping("/{id}/comments")
    public Object addComment(
            @PathVariable Long id,
            @RequestBody CommentCreateRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        Long userId = extractUserId(httpRequest);
        if (userId == null) {
            response.setStatus(401);
            return Map.of("error", "login_required");
        }

        try {
            GalleryComment comment = galleryService.addComment(id, userId, request.getContent());
            return GalleryMapper.toCommentDto(comment);
        } catch (Exception ex) {
            response.setStatus(500);
            return Map.of("error", ex.getMessage());
        }
    }

    @DeleteMapping("/comments/{commentId}")
    public Object deleteComment(
            @PathVariable Long commentId,
            HttpServletRequest request,
            HttpServletResponse response) {
        Long userId = extractUserId(request);
        if (userId == null) {
            response.setStatus(401);
            return Map.of("error", "login_required");
        }

        try {
            galleryService.deleteComment(commentId, userId);
            return Map.of("success", true);
        } catch (Exception ex) {
            response.setStatus(400);
            return Map.of("error", ex.getMessage());
        }
    }

    @PostMapping
    public Object createGallery(
            @RequestBody GalleryCreateRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        String accessToken = readCookie(httpRequest, "access_token");
        if (accessToken == null || accessToken.isBlank()) {
            response.setStatus(401);
            return "no_access_token";
        }

        try {
            Long userId = tokenService.verifyAndExtractUserId(accessToken);
            Gallery saved = galleryService.createGallery(
                    userId,
                    request.getTitle(),
                    request.getDescription(),
                    request.getImageUrls());
            return GalleryMapper.toDto(saved);
        } catch (IllegalArgumentException ex) {
            response.setStatus(401);
            return "invalid_token";
        } catch (Exception ex) {
            response.setStatus(500);
            return "failed_to_create_gallery: " + ex.getMessage();
        }
    }

    private Long extractUserId(HttpServletRequest request) {
        String accessToken = readCookie(request, "access_token");
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }
        try {
            return tokenService.verifyAndExtractUserId(accessToken);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null)
            return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName()))
                return c.getValue();
        }
        return null;
    }
}

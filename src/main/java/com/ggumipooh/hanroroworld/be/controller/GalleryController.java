package com.ggumipooh.hanroroworld.be.controller;

import com.ggumipooh.hanroroworld.be.dto.GalleryCommentDto;
import com.ggumipooh.hanroroworld.be.dto.GalleryCreateRequest;
import com.ggumipooh.hanroroworld.be.dto.GalleryDto;
import com.ggumipooh.hanroroworld.be.dto.mapper.GalleryMapper;
import com.ggumipooh.hanroroworld.be.model.Gallery;
import com.ggumipooh.hanroroworld.be.model.GalleryComment;
import com.ggumipooh.hanroroworld.be.security.SecurityUtil;
import com.ggumipooh.hanroroworld.be.service.GalleryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/gallery")
public class GalleryController {

    private final GalleryService galleryService;

    // 갤러리 목록 조회
    @GetMapping
    public ResponseEntity<Page<GalleryDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {
        return ResponseEntity.ok(galleryService.getGalleryPage(page, size));
    }

    // 갤러리 검색
    @GetMapping("/search")
    public ResponseEntity<Page<GalleryDto>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(galleryService.searchByKeyword(keyword, page, size));
    }

    // 갤러리 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();

        galleryService.incrementViewCount(id);

        return galleryService.getById(id, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 갤러리 생성
    @PostMapping
    public ResponseEntity<?> create(@RequestBody GalleryCreateRequest req) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body("unauthorized");
        }

        if (req.getTitle() == null || req.getTitle().isBlank()) {
            return ResponseEntity.badRequest().body("title is required");
        }
        if (req.getImageUrls() == null || req.getImageUrls().isEmpty()) {
            return ResponseEntity.badRequest().body("at least one image is required");
        }

        Gallery gallery = galleryService.create(
                userId,
                req.getTitle().trim(),
                req.getDescription(),
                req.getImageUrls());

        return ResponseEntity.ok(Map.of("id", gallery.getId()));
    }

    // 갤러리 좋아요 토글
    @PostMapping("/{id}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body("unauthorized");
        }

        Map<String, Object> result = galleryService.toggleLike(id, userId);
        return ResponseEntity.ok(result);
    }

    // 갤러리 댓글 작성
    @PostMapping("/{id}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable Long id,
            @RequestBody Map<String, String> req) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body("unauthorized");
        }

        String content = req.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body("content is required");
        }

        GalleryComment comment = galleryService.addComment(id, userId, content.trim());
        GalleryCommentDto dto = GalleryMapper.toCommentDto(comment);

        return ResponseEntity.ok(dto);
    }

    // 갤러리 댓글 삭제
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body("unauthorized");
        }

        try {
            galleryService.deleteComment(commentId, userId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 갤러리 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGallery(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body("unauthorized");
        }

        try {
            galleryService.deleteGallery(id, userId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

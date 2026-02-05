package com.ggumipooh.hanroroworld.be.controller;

import com.ggumipooh.hanroroworld.be.dto.GalleryCommentDto;
import com.ggumipooh.hanroroworld.be.dto.GalleryCreateRequest;
import com.ggumipooh.hanroroworld.be.dto.GalleryDetailDto;
import com.ggumipooh.hanroroworld.be.dto.GalleryDto;
import com.ggumipooh.hanroroworld.be.dto.mapper.GalleryMapper;
import com.ggumipooh.hanroroworld.be.model.Gallery;
import com.ggumipooh.hanroroworld.be.model.GalleryComment;
import com.ggumipooh.hanroroworld.be.model.User;
import com.ggumipooh.hanroroworld.be.service.GalleryService;
import com.ggumipooh.hanroroworld.be.service.TokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
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
    private final TokenService tokenService;

    /**
     * 갤러리 목록 조회
     */
    @GetMapping
    public ResponseEntity<Page<GalleryDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {
        return ResponseEntity.ok(galleryService.getGalleryPage(page, size));
    }

    /**
     * 갤러리 검색
     */
    @GetMapping("/search")
    public ResponseEntity<Page<GalleryDto>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(galleryService.searchByKeyword(keyword, page, size));
    }

    /**
     * 갤러리 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable Long id, HttpServletRequest request) {
        User user = extractUser(request);
        Long userId = user != null ? user.getId() : null;

        galleryService.incrementViewCount(id);

        return galleryService.getById(id, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 갤러리 생성
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody GalleryCreateRequest req, HttpServletRequest request) {
        User user = extractUser(request);
        if (user == null) {
            return ResponseEntity.status(401).body("unauthorized");
        }

        if (req.getTitle() == null || req.getTitle().isBlank()) {
            return ResponseEntity.badRequest().body("title is required");
        }
        if (req.getImageUrls() == null || req.getImageUrls().isEmpty()) {
            return ResponseEntity.badRequest().body("at least one image is required");
        }

        Gallery gallery = galleryService.create(
                user.getId(),
                req.getTitle().trim(),
                req.getDescription(),
                req.getImageUrls());

        return ResponseEntity.ok(Map.of("id", gallery.getId()));
    }

    /**
     * 좋아요 토글
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long id, HttpServletRequest request) {
        User user = extractUser(request);
        if (user == null) {
            return ResponseEntity.status(401).body("unauthorized");
        }

        boolean liked = galleryService.toggleLike(id, user.getId());
        return ResponseEntity.ok(Map.of("liked", liked));
    }

    /**
     * 댓글 작성
     */
    @PostMapping("/{id}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable Long id,
            @RequestBody Map<String, String> req,
            HttpServletRequest request) {
        User user = extractUser(request);
        if (user == null) {
            return ResponseEntity.status(401).body("unauthorized");
        }

        String content = req.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body("content is required");
        }

        GalleryComment comment = galleryService.addComment(id, user.getId(), content.trim());
        GalleryCommentDto dto = GalleryMapper.toCommentDto(comment);

        return ResponseEntity.ok(dto);
    }

    /**
     * 댓글 삭제
     */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId, HttpServletRequest request) {
        User user = extractUser(request);
        if (user == null) {
            return ResponseEntity.status(401).body("unauthorized");
        }

        try {
            galleryService.deleteComment(commentId, user.getId());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private User extractUser(HttpServletRequest request) {
        String token = readCookie(request, "access_token");
        if (token == null)
            return null;
        try {
            return tokenService.verifyAndExtractUser(token);
        } catch (Exception e) {
            return null;
        }
    }

    private String readCookie(HttpServletRequest request, String name) {
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

package com.ggumipooh.hanroroworld.be.controller;

import com.ggumipooh.hanroroworld.be.dto.CommentDto;
import com.ggumipooh.hanroroworld.be.dto.CommentRequest;
import com.ggumipooh.hanroroworld.be.service.CommentService;
import com.ggumipooh.hanroroworld.be.service.TokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/song")
public class SongController {

    private final CommentService commentService;
    private final TokenService tokenService;

    @PostMapping("/{songId}/comment")
    public Object createComment(
            @PathVariable Long songId,
            @RequestBody CommentRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        String accessToken = readCookie(httpRequest, "access_token");
        if (accessToken == null || accessToken.isBlank()) {
            response.setStatus(401);
            return "unauthorized";
        }

        try {
            Long userId = tokenService.verifyAndExtractUserId(accessToken);
            CommentDto saved = commentService.createComment(songId, userId, request.getComment());
            return saved;
        } catch (IllegalArgumentException ex) {
            response.setStatus(401);
            return "invalid_token";
        } catch (Exception ex) {
            response.setStatus(500);
            return "failed_to_save_comment";
        }
    }

    @GetMapping("/{songId}/comments")
    public List<CommentDto> getComments(@PathVariable Long songId) {
        return commentService.getCommentsBySong(songId);
    }

    @DeleteMapping("/comment/{commentId}")
    public Object deleteComment(
            @PathVariable Long commentId,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        String accessToken = readCookie(httpRequest, "access_token");
        if (accessToken == null || accessToken.isBlank()) {
            response.setStatus(401);
            return "unauthorized";
        }

        try {
            Long userId = tokenService.verifyAndExtractUserId(accessToken);
            boolean deleted = commentService.deleteComment(commentId, userId);
            if (!deleted) {
                response.setStatus(403);
                return "not_allowed";
            }
            return "ok";
        } catch (IllegalArgumentException ex) {
            response.setStatus(401);
            return "invalid_token";
        } catch (Exception ex) {
            response.setStatus(500);
            return "failed_to_delete_comment";
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

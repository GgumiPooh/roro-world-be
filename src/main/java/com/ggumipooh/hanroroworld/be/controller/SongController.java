package com.ggumipooh.hanroroworld.be.controller;

import com.ggumipooh.hanroroworld.be.dto.CommentDto;
import com.ggumipooh.hanroroworld.be.dto.CommentRequest;
import com.ggumipooh.hanroroworld.be.security.SecurityUtil;
import com.ggumipooh.hanroroworld.be.service.CommentService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/song")
public class SongController {

    private final CommentService commentService;

    @PostMapping("/{songId}/comment")
    public Object createComment(
            @PathVariable Long songId,
            @RequestBody CommentRequest request,
            HttpServletResponse response) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            response.setStatus(401);
            return "unauthorized";
        }

        try {
            return commentService.createComment(songId, userId, request.getContent());
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
            HttpServletResponse response) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            response.setStatus(401);
            return "unauthorized";
        }

        try {
            boolean deleted = commentService.deleteComment(commentId, userId);
            if (!deleted) {
                response.setStatus(403);
                return "not_allowed";
            }
            return "ok";
        } catch (Exception ex) {
            response.setStatus(500);
            return "failed_to_delete_comment";
        }
    }
}

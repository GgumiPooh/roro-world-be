package com.ggumipooh.hanroroworld.be.controller;

import com.ggumipooh.hanroroworld.be.dto.MessageDto;
import com.ggumipooh.hanroroworld.be.dto.MessageRequest;
import com.ggumipooh.hanroroworld.be.security.SecurityUtil;
import com.ggumipooh.hanroroworld.be.service.MessageService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/message")
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    public Object createMessage(
            @RequestBody MessageRequest request,
            HttpServletResponse response) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            response.setStatus(401);
            return "unauthorized";
        }

        try {
            return messageService.createMessage(userId, request.getContent());
        } catch (Exception ex) {
            response.setStatus(500);
            return "failed_to_save_message";
        }
    }

    @GetMapping
    public List<MessageDto> getMessages() {
        return messageService.getAllMessages();
    }

    @DeleteMapping("/{messageId}")
    public Object deleteMessage(
            @PathVariable Long messageId,
            HttpServletResponse response) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            response.setStatus(401);
            return "unauthorized";
        }

        try {
            boolean deleted = messageService.deleteMessage(messageId, userId);
            if (!deleted) {
                response.setStatus(403);
                return "not_allowed";
            }
            return "ok";
        } catch (Exception ex) {
            response.setStatus(500);
            return "failed_to_delete_message";
        }
    }
}

package com.ggumipooh.hanroroworld.be.controller;

import com.ggumipooh.hanroroworld.be.dto.MessageDto;
import com.ggumipooh.hanroroworld.be.dto.MessageRequest;
import com.ggumipooh.hanroroworld.be.service.MessageService;
import com.ggumipooh.hanroroworld.be.service.TokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/message")
public class MessageController {

    private final MessageService messageService;
    private final TokenService tokenService;

    @PostMapping
    public Object createMessage(
            @RequestBody MessageRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        String accessToken = readCookie(httpRequest, "access_token");
        if (accessToken == null || accessToken.isBlank()) {
            response.setStatus(401);
            return "unauthorized";
        }

        try {
            Long userId = tokenService.verifyAndExtractUserId(accessToken);
            MessageDto saved = messageService.createMessage(userId, request.getComment());
            return saved;
        } catch (IllegalArgumentException ex) {
            response.setStatus(401);
            return "invalid_token";
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
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        String accessToken = readCookie(httpRequest, "access_token");
        if (accessToken == null || accessToken.isBlank()) {
            response.setStatus(401);
            return "unauthorized";
        }

        try {
            Long userId = tokenService.verifyAndExtractUserId(accessToken);
            boolean deleted = messageService.deleteMessage(messageId, userId);
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
            return "failed_to_delete_message";
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


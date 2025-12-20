package com.ggumipooh.hanroroworld.be.service;

import com.ggumipooh.hanroroworld.be.dto.MessageDto;
import com.ggumipooh.hanroroworld.be.model.MessageToArtist;
import com.ggumipooh.hanroroworld.be.model.User;
import com.ggumipooh.hanroroworld.be.repository.MessageToArtistRepository;
import com.ggumipooh.hanroroworld.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageToArtistRepository messageRepository;
    private final UserRepository userRepository;

    @Transactional
    public MessageDto createMessage(Long userId, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        MessageToArtist message = MessageToArtist.builder()
                .message(content)
                .authorName(user.getNickname())
                .user(user)
                .build();

        MessageToArtist saved = messageRepository.save(message);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getAllMessages() {
        return messageRepository.findAllByOrderByMessagedAtDesc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean deleteMessage(Long messageId, Long userId) {
        MessageToArtist message = messageRepository.findById(messageId).orElse(null);
        if (message == null) {
            return false;
        }
        if (message.getUser() == null || !message.getUser().getId().equals(userId)) {
            return false;
        }
        messageRepository.delete(message);
        return true;
    }

    private MessageDto toDto(MessageToArtist message) {
        String authorName = message.getAuthorName() != null ? message.getAuthorName() : "익명";
        String createdAtStr = "";
        if (message.getMessagedAt() != null) {
            var d = message.getMessagedAt();
            createdAtStr = String.format("%d.%02d.%02d %02d:%02d",
                    d.getYear(), d.getMonthValue(), d.getDayOfMonth(),
                    d.getHour(), d.getMinute());
        }

        return MessageDto.builder()
                .id(message.getId())
                .message(message.getMessage())
                .author(authorName)
                .createdAt(createdAtStr)
                .build();
    }
}


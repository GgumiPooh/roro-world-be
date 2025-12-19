package com.ggumipooh.hanroroworld.be.service;

import com.ggumipooh.hanroroworld.be.dto.CommentDto;
import com.ggumipooh.hanroroworld.be.model.CommentSong;
import com.ggumipooh.hanroroworld.be.model.Song;
import com.ggumipooh.hanroroworld.be.model.User;
import com.ggumipooh.hanroroworld.be.repository.CommentSongRepository;
import com.ggumipooh.hanroroworld.be.repository.SongRepository;
import com.ggumipooh.hanroroworld.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

        private final CommentSongRepository commentSongRepository;
        private final SongRepository songRepository;
        private final UserRepository userRepository;

        @Transactional
        public CommentDto createComment(Long songId, Long userId, String content) {
                Song song = songRepository.findById(songId)
                                .orElseThrow(() -> new IllegalArgumentException("Song not found: " + songId));
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

                CommentSong comment = CommentSong.builder()
                                .comment(content)
                                .song(song)
                                .user(user)
                                .build();

                CommentSong saved = commentSongRepository.save(comment);

                return toDto(saved);
        }

        @Transactional(readOnly = true)
        public List<CommentDto> getCommentsBySong(Long songId) {
                return commentSongRepository.findBySongIdOrderByCreatedAtDesc(songId)
                                .stream()
                                .map(this::toDto)
                                .collect(Collectors.toList());
        }

        @Transactional
        public boolean deleteComment(Long commentId, Long userId) {
                CommentSong comment = commentSongRepository.findById(commentId).orElse(null);
                if (comment == null) {
                        return false;
                }
                // Only allow deletion if the user owns the comment
                if (comment.getUser() == null || !comment.getUser().getId().equals(userId)) {
                        return false;
                }
                commentSongRepository.delete(comment);
                return true;
        }

        private CommentDto toDto(CommentSong comment) {
                String authorName = comment.getUser() != null && comment.getUser().getNickname() != null
                                ? comment.getUser().getNickname()
                                : "익명";
                String createdAtStr = "";
                if (comment.getCreatedAt() != null) {
                        var d = comment.getCreatedAt();
                        createdAtStr = String.format("%d.%02d.%02d", d.getYear(), d.getMonthValue(), d.getDayOfMonth());
                }

                return CommentDto.builder()
                                .id(comment.getId())
                                .comment(comment.getComment())
                                .author(authorName)
                                .createdAt(createdAtStr)
                                .build();
        }
}

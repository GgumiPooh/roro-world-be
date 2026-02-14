package com.ggumipooh.hanroroworld.be.service;

import com.ggumipooh.hanroroworld.be.dto.mapper.GalleryMapper;
import com.ggumipooh.hanroroworld.be.dto.GalleryDetailDto;
import com.ggumipooh.hanroroworld.be.dto.GalleryDto;

import java.util.Map;
import com.ggumipooh.hanroroworld.be.model.Gallery;
import com.ggumipooh.hanroroworld.be.model.GalleryComment;
import com.ggumipooh.hanroroworld.be.model.GalleryImage;
import com.ggumipooh.hanroroworld.be.model.GalleryLike;
import com.ggumipooh.hanroroworld.be.model.User;
import com.ggumipooh.hanroroworld.be.repository.GalleryCommentRepository;
import com.ggumipooh.hanroroworld.be.repository.GalleryLikeRepository;
import com.ggumipooh.hanroroworld.be.repository.GalleryRepository;
import com.ggumipooh.hanroroworld.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GalleryService {

    private final GalleryRepository galleryRepository;
    private final GalleryCommentRepository commentRepository;
    private final GalleryLikeRepository likeRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<GalleryDto> getGalleryPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return galleryRepository.findAllByOrderByCreatedAtDesc(pageable).map(gallery -> GalleryMapper.toDto(gallery,
                (int) getCommentCount(gallery.getId())));
    }

    @Transactional(readOnly = true)
    public Page<GalleryDto> searchByKeyword(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return galleryRepository.searchByTitle(keyword, pageable).map(gallery -> GalleryMapper.toDto(gallery,
                (int) getCommentCount(gallery.getId())));
    }

    @Transactional(readOnly = true)
    public Optional<GalleryDetailDto> getById(Long id, Long userId) {
        return galleryRepository.findById(id)
                .map(gallery -> GalleryMapper.toDetailDto(gallery, getComments(gallery.getId()),
                        isLikedByUser(id, userId)));
    }

    @Transactional
    public GalleryDto save(GalleryDto galleryDto) {
        if (galleryDto.getAuthorId() == null) {
            throw new IllegalArgumentException("Author ID is required");
        }

        User user = userRepository.findById(galleryDto.getAuthorId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Gallery gallery = GalleryMapper.toEntity(galleryDto, user);
        Gallery savedGallery = galleryRepository.save(gallery);

        return GalleryMapper.toDto(savedGallery);
    }

    @Transactional
    public Gallery create(Long userId, String title, String description, List<String> imageUrls) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Gallery gallery = Gallery.builder()
                .title(title)
                .description(description)
                .user(user)
                .build();

        // 이미지 추가
        for (int i = 0; i < imageUrls.size(); i++) {
            GalleryImage image = GalleryImage.builder()
                    .imageUrl(imageUrls.get(i))
                    .displayOrder(i)
                    .build();
            gallery.addImage(image);
        }

        return galleryRepository.save(gallery);
    }

    @Transactional
    public void incrementViewCount(Long id) {
        galleryRepository.findById(id).ifPresent(gallery -> {
            gallery.incrementViewCount();
            galleryRepository.save(gallery);
        });
    }

    // ===== 좋아요 기능 =====

    @Transactional(readOnly = true)
    public boolean isLikedByUser(Long galleryId, Long userId) {
        if (userId == null)
            return false;
        return likeRepository.existsByGalleryIdAndUserId(galleryId, userId);
    }

    @Transactional
    public Map<String, Object> toggleLike(Long galleryId, Long userId) {
        Gallery gallery = galleryRepository.findById(galleryId)
                .orElseThrow(() -> new IllegalArgumentException("Gallery not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Optional<GalleryLike> existingLike = likeRepository.findByGalleryIdAndUserId(galleryId, userId);

        boolean liked;
        if (existingLike.isPresent()) {
            // 이미 좋아요 -> 취소
            likeRepository.delete(existingLike.get());
            gallery.decrementLikeCount();
            liked = false;
        } else {
            // 좋아요 추가
            GalleryLike like = GalleryLike.builder()
                    .gallery(gallery)
                    .user(user)
                    .build();
            likeRepository.save(like);
            gallery.incrementLikeCount();
            liked = true;
        }
        galleryRepository.save(gallery);
        
        return Map.of(
                "liked", liked,
                "likeCount", gallery.getLikeCount()
        );
    }

    // ===== 댓글 기능 =====

    @Transactional(readOnly = true)
    public List<GalleryComment> getComments(Long galleryId) {
        return commentRepository.findByGalleryIdOrderByCreatedAtDesc(galleryId);
    }

    @Transactional(readOnly = true)
    public long getCommentCount(Long galleryId) {
        return commentRepository.countByGalleryId(galleryId);
    }

    @Transactional
    public GalleryComment addComment(Long galleryId, Long userId, String content) {
        Gallery gallery = galleryRepository.findById(galleryId)
                .orElseThrow(() -> new IllegalArgumentException("Gallery not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        GalleryComment comment = GalleryComment.builder()
                .content(content)
                .gallery(gallery)
                .user(user)
                .build();

        return commentRepository.save(comment);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        GalleryComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Not authorized to delete this comment");
        }

        commentRepository.delete(comment);
    }

    // ===== 갤러리 삭제 =====

    @Transactional
    public void deleteGallery(Long galleryId, Long userId) {
        Gallery gallery = galleryRepository.findById(galleryId)
                .orElseThrow(() -> new IllegalArgumentException("Gallery not found"));

        if (!gallery.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Not authorized to delete this gallery");
        }

        // 연관된 좋아요, 댓글은 cascade로 삭제됨
        galleryRepository.delete(gallery);
    }
}

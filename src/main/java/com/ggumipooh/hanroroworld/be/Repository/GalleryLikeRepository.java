package com.ggumipooh.hanroroworld.be.repository;

import com.ggumipooh.hanroroworld.be.model.GalleryLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GalleryLikeRepository extends JpaRepository<GalleryLike, Long> {

    Optional<GalleryLike> findByGalleryIdAndUserId(Long galleryId, Long userId);

    boolean existsByGalleryIdAndUserId(Long galleryId, Long userId);

    long countByGalleryId(Long galleryId);
}


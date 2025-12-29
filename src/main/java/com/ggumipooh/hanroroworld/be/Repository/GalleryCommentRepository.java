package com.ggumipooh.hanroroworld.be.repository;

import com.ggumipooh.hanroroworld.be.model.GalleryComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GalleryCommentRepository extends JpaRepository<GalleryComment, Long> {

    List<GalleryComment> findByGalleryIdOrderByCreatedAtDesc(Long galleryId);

    long countByGalleryId(Long galleryId);
}


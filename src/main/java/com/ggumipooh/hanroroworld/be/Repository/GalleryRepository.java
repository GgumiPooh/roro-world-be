package com.ggumipooh.hanroroworld.be.repository;

import com.ggumipooh.hanroroworld.be.model.Gallery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GalleryRepository extends JpaRepository<Gallery, Long> {

    Page<Gallery> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT g FROM Gallery g WHERE g.title LIKE %:keyword% ORDER BY g.createdAt DESC")
    Page<Gallery> searchByTitle(@Param("keyword") String keyword, Pageable pageable);

    List<Gallery> findByUserIdOrderByCreatedAtDesc(Long userId);
}

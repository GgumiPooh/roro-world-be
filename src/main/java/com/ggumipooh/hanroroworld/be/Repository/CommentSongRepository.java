package com.ggumipooh.hanroroworld.be.repository;

import com.ggumipooh.hanroroworld.be.model.CommentSong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentSongRepository extends JpaRepository<CommentSong, Long> {
    List<CommentSong> findBySongIdOrderByCommentedAtDesc(Long songId);
}

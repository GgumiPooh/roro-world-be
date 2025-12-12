package com.ggumipooh.hanroroworld.be.Repository;

import com.ggumipooh.hanroroworld.be.model.CommentSong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentSongRepository extends JpaRepository<CommentSong, Long> {
}

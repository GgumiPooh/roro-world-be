package com.ggumipooh.hanroroworld.be.Repository;

import com.ggumipooh.hanroroworld.be.model.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SongRepository extends JpaRepository<Song, Long> {
}

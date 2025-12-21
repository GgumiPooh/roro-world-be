package com.ggumipooh.hanroroworld.be.repository;

import com.ggumipooh.hanroroworld.be.model.MessageToArtist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageToArtistRepository extends JpaRepository<MessageToArtist, Long> {
    List<MessageToArtist> findAllByOrderByMessagedAtDesc();
}


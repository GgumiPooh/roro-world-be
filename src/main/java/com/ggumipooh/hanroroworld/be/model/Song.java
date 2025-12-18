package com.ggumipooh.hanroroworld.be.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "songs")
public class Song extends BaseEntity {

    @Column
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Map<String, String>> title;

    @Column
    @JdbcTypeCode(SqlTypes.JSON)
    @ElementCollection
    private List<LanguageData> lyrics;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id")
    private Album album;

    @Column
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Metadata> metadata;

    @OneToMany(mappedBy = "song", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserFavoriteSong> userFavorites = new ArrayList<>();

    @OneToMany(mappedBy = "song", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentSong> comments = new ArrayList<>();
}

package com.ggumipooh.hanroroworld.be.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "song")
@Getter
@Setter
public class Song extends BaseEntity {

    @Column
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Map<String, String>> title;

    @Column
    @JdbcTypeCode(SqlTypes.JSON)
    private List<LanguageData> description;

    @Column
    @JdbcTypeCode(SqlTypes.JSON)
    private List<LanguageData> lyrics;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "albumId")
    private Album album;

    @Column
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Metadata> metadata;

    @OneToMany(mappedBy = "song", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserFavoriteSong> userFavorites = new ArrayList<>();

    @OneToMany(mappedBy = "song", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentSong> comments = new ArrayList<>();
}

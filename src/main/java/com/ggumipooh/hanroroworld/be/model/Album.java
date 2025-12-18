package com.ggumipooh.hanroroworld.be.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Setter
@Getter
public class Album extends BaseEntity {

    @Column
    @JdbcTypeCode(SqlTypes.JSON)
    private List<LanguageData> title;

    @Column
    private LocalDate publishedAt;

    @Column
    private String type;

    @Column
    @JdbcTypeCode(SqlTypes.JSON)
    private List<LanguageData> description;

    @Column
    @JdbcTypeCode(SqlTypes.JSON)
    @JsonAlias("metaData") // accept both "metadata" and "metaData" from JSON
    private List<Metadata> metadata;

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Song> songs = new ArrayList<>();
}

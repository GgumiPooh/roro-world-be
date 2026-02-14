package com.ggumipooh.hanroroworld.be.dto;

import com.ggumipooh.hanroroworld.be.model.LanguageData;
import com.ggumipooh.hanroroworld.be.model.Metadata;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SongDto {
    private Long id;
    private List<Map<String, String>> title;
    private List<LanguageData> description;
    private List<LanguageData> lyrics;
    private List<Metadata> metadata;
    private Long albumId;
    private Integer trackNumber;
}

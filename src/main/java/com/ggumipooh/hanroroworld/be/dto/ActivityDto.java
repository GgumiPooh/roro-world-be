package com.ggumipooh.hanroroworld.be.dto;

import com.ggumipooh.hanroroworld.be.model.LanguageData;
import com.ggumipooh.hanroroworld.be.model.Metadata;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
public class ActivityDto {
    private Long id;
    private List<LanguageData> title;
    private List<LanguageData> description;
    private String activityType;
    private List<Metadata> metaData;
    private LocalDate activeFrom;
    private LocalDate activeTo;
}

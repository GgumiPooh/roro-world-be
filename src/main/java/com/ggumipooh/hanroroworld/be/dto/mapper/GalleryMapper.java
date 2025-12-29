package com.ggumipooh.hanroroworld.be.dto.mapper;

import com.ggumipooh.hanroroworld.be.dto.GalleryCommentDto;
import com.ggumipooh.hanroroworld.be.dto.GalleryDetailDto;
import com.ggumipooh.hanroroworld.be.dto.GalleryDto;
import com.ggumipooh.hanroroworld.be.model.Gallery;
import com.ggumipooh.hanroroworld.be.model.GalleryComment;
import com.ggumipooh.hanroroworld.be.model.GalleryImage;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class GalleryMapper {
    private GalleryMapper() {
    }

    public static GalleryDto toDto(Gallery gallery) {
        return toDto(gallery, 0);
    }

    public static GalleryDto toDto(Gallery gallery, int commentCount) {
        if (gallery == null) {
            return null;
        }

        List<String> imageUrls = gallery.getImages().stream()
                .sorted(Comparator.comparing(GalleryImage::getDisplayOrder))
                .map(GalleryImage::getImageUrl)
                .toList();

        return GalleryDto.builder()
                .id(gallery.getId())
                .title(gallery.getTitle())
                .description(gallery.getDescription())
                .authorName(gallery.getUser() != null ? gallery.getUser().getNickname() : "익명")
                .authorId(gallery.getUser() != null ? gallery.getUser().getId() : null)
                .imageUrls(imageUrls)
                .likeCount(gallery.getLikeCount())
                .viewCount(gallery.getViewCount())
                .commentCount(commentCount)
                .createdAt(gallery.getCreatedAt())
                .build();
    }

    public static GalleryDetailDto toDetailDto(Gallery gallery, List<GalleryComment> comments, boolean isLikedByMe) {
        if (gallery == null) {
            return null;
        }

        List<String> imageUrls = gallery.getImages().stream()
                .sorted(Comparator.comparing(GalleryImage::getDisplayOrder))
                .map(GalleryImage::getImageUrl)
                .toList();

        List<GalleryCommentDto> commentDtos = comments == null ? List.of()
                : comments.stream()
                        .map(GalleryMapper::toCommentDto)
                        .toList();

        return GalleryDetailDto.builder()
                .id(gallery.getId())
                .title(gallery.getTitle())
                .description(gallery.getDescription())
                .authorName(gallery.getUser() != null ? gallery.getUser().getNickname() : "익명")
                .imageUrls(imageUrls)
                .likeCount(gallery.getLikeCount())
                .viewCount(gallery.getViewCount())
                .commentCount(commentDtos.size())
                .isLikedByMe(isLikedByMe)
                .comments(commentDtos)
                .createdAt(gallery.getCreatedAt())
                .build();
    }

    public static GalleryCommentDto toCommentDto(GalleryComment comment) {
        if (comment == null) {
            return null;
        }

        return GalleryCommentDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorName(comment.getUser() != null ? comment.getUser().getNickname() : "익명")
                .createdAt(comment.getCreatedAt())
                .build();
    }

    public static List<GalleryDto> toDtoList(List<Gallery> galleries) {
        return galleries == null ? List.of()
                : galleries.stream()
                        .filter(Objects::nonNull)
                        .map(GalleryMapper::toDto)
                        .toList();
    }
}


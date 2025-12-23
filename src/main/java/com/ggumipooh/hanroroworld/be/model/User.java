package com.ggumipooh.hanroroworld.be.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_provider_provider_id", columnNames = { "provider", "providerId" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column
    private String name;

    @Column(unique = true)
    private String nickname;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserFavoriteSong favoriteSong;

    @Column
    private String provider;

    @Column
    private String providerId;
}

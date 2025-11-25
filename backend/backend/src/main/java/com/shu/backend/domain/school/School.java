package com.shu.backend.domain.school;

import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class School extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "logo_image_url")
    private String logoImageUrl;

}

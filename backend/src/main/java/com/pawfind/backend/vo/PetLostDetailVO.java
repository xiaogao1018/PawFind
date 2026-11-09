
package com.pawfind.backend.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PetLostDetailVO {

    private Long id;
    private Long userId;

    private String petName;
    private String species;
    private String description;

    private LocalDateTime lostTime;
    private String lostLocation;

    private String status;
    private LocalDateTime createdAt;
}

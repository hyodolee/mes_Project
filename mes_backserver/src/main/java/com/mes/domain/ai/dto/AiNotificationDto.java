package com.mes.domain.ai.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiNotificationDto {
    private Long id;
    private String title;
    private String message;
    private String severity;
    private String sourceRef;
    private boolean read;
    private LocalDateTime createdAt;
}

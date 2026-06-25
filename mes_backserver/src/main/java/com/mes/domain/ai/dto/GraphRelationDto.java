package com.mes.domain.ai.dto;

public record GraphRelationDto(
        String from,
        String to,
        String type,
        String label
) {
}

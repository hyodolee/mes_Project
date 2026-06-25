package com.mes.domain.ai.dto;

import java.util.List;

public record GraphPathDto(
        String title,
        String summary,
        List<GraphNodeDto> nodes,
        List<GraphRelationDto> relations
) {
}

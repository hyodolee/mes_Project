package com.mes.application.service.ai.rag;

import com.mes.domain.ai.dto.GraphNodeDto;
import com.mes.domain.ai.dto.GraphPathDto;
import com.mes.domain.ai.dto.GraphRelationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationGraphSearchService {

    private static final int MAX_RESULTS = 5;

    private final Neo4jClient neo4jClient;

    @Value("${ai.graph-rag.enabled:true}")
    private boolean graphRagEnabled;

    public List<GraphPathDto> search(String query) {
        if (!graphRagEnabled) {
            return List.of();
        }
        if (query == null || query.isBlank()) {
            return List.of();
        }

        try {
            Collection<Map<String, Object>> rows = neo4jClient.query("""
                    MATCH (start:OperationConcept)
                    WHERE any(keyword IN coalesce(start.keywords, [])
                        WHERE toLower($query) CONTAINS toLower(keyword))
                       OR toLower(start.code) CONTAINS toLower($query)
                       OR toLower(start.name) CONTAINS toLower($query)
                    MATCH path = (start)-[:REQUIRES|MAPPED_TO|HANDLED_BY|CHECKS|INDICATES*1..4]-(end)
                    RETURN
                        [node IN nodes(path) | {
                            code: node.code,
                            type: node.type,
                            name: node.name
                        }] AS nodes,
                        [rel IN relationships(path) | {
                            from: startNode(rel).code,
                            to: endNode(rel).code,
                            type: type(rel),
                            label: rel.label
                        }] AS relations
                    LIMIT $limit
                    """)
                    .bind(query)
                    .to("query")
                    .bind(MAX_RESULTS)
                    .to("limit")
                    .fetch()
                    .all();

            return rows.stream()
                    .map(this::toGraphPath)
                    .filter(path -> !path.nodes().isEmpty())
                    .toList();
        } catch (Exception e) {
            log.warn("[GRAPH-RAG] Neo4j graph search failed: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private GraphPathDto toGraphPath(Map<String, Object> row) {
        List<Map<String, Object>> nodeRows = row.get("nodes") instanceof List<?>
                ? (List<Map<String, Object>>) row.get("nodes")
                : List.of();
        List<Map<String, Object>> relationRows = row.get("relations") instanceof List<?>
                ? (List<Map<String, Object>>) row.get("relations")
                : List.of();

        List<GraphNodeDto> nodes = nodeRows.stream()
                .map(this::toNode)
                .toList();
        List<GraphRelationDto> relations = relationRows.stream()
                .map(this::toRelation)
                .toList();

        return new GraphPathDto(
                title(nodes),
                summarize(nodes, relations),
                nodes,
                relations
        );
    }

    private GraphNodeDto toNode(Map<String, Object> node) {
        return new GraphNodeDto(
                value(node.get("code")),
                value(node.get("type")),
                value(node.get("name"))
        );
    }

    private GraphRelationDto toRelation(Map<String, Object> relation) {
        return new GraphRelationDto(
                value(relation.get("from")),
                value(relation.get("to")),
                value(relation.get("type")),
                value(relation.get("label"))
        );
    }

    private String title(List<GraphNodeDto> nodes) {
        if (nodes.isEmpty()) {
            return "Graph RAG 관계 경로";
        }
        return "Graph RAG 관계 경로: " + nodes.get(0).name();
    }

    private String summarize(List<GraphNodeDto> nodes, List<GraphRelationDto> relations) {
        if (nodes.isEmpty()) {
            return "관련 관계 경로가 없습니다.";
        }

        Set<String> names = new LinkedHashSet<>();
        for (GraphNodeDto node : nodes) {
            if (!node.name().isBlank()) {
                names.add(node.name());
            }
        }

        List<String> relationLabels = new ArrayList<>();
        for (GraphRelationDto relation : relations) {
            if (!relation.label().isBlank()) {
                relationLabels.add(relation.label());
            }
        }

        String pathSummary = String.join(" -> ", names);
        if (relationLabels.isEmpty()) {
            return pathSummary;
        }
        return pathSummary + " (" + String.join(", ", relationLabels) + ")";
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}

package com.mcs.application.service.route;

import com.mcs.domain.route.dto.RouteEdgeDto;
import com.mcs.domain.route.dto.RouteNodeDto;
import com.mcs.domain.route.dto.RouteOptimizeRequest;
import com.mcs.domain.route.dto.RouteOptimizeResultDto;
import com.mcs.domain.route.dto.RouteSearchDto;
import com.mcs.domain.route.dto.RouteStepDto;
import com.mcs.domain.route.dto.TransferRouteDto;
import com.mcs.domain.transfer.dto.TransferOrderDto;
import com.mcs.global.exception.BusinessException;
import com.mcs.global.exception.ErrorCode;
import com.mcs.infra.persistence.mybatis.mapper.route.RouteMapper;
import com.mcs.infra.persistence.mybatis.mapper.transfer.TransferMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouteService {

    private static final Set<String> EXCLUDED_STATUSES = Set.of("BLOCKED", "INTERLOCKED", "MAINTENANCE");
    private static final double CONGESTION_AVOIDANCE_PENALTY = 1000.0;

    private final RouteMapper routeMapper;
    private final TransferMapper transferMapper;

    public List<RouteNodeDto> getRouteNodes(RouteSearchDto searchDto) {
        return routeMapper.selectRouteNodes(searchDto);
    }

    public List<RouteEdgeDto> getRouteEdges(RouteSearchDto searchDto) {
        return routeMapper.selectRouteEdges(searchDto);
    }

    public RouteOptimizeResultDto optimize(RouteOptimizeRequest request) {
        String optimizeRule = normalizeOptimizeRule(request.optimizeRule());
        validateOptimizeRequest(request);

        RouteNodeDto startNode = routeMapper.selectRouteNodeByLocation(request.plantCd(), request.fromLocationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "출발 Location에 연결된 경로 노드가 없습니다."));
        RouteNodeDto endNode = routeMapper.selectRouteNodeByLocation(request.plantCd(), request.toLocationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "도착 Location에 연결된 경로 노드가 없습니다."));

        List<RouteEdgeDto> edges = routeMapper.selectUsableRouteEdges(request.plantCd()).stream()
                .filter(edge -> !EXCLUDED_STATUSES.contains(edge.edgeStatus()))
                .toList();

        Map<Long, List<RouteLeg>> graph = buildGraph(edges, optimizeRule);
        List<RouteLeg> path = findShortestPath(startNode.routeNodeId(), endNode.routeNodeId(), graph);

        if (path.isEmpty()) {
            return new RouteOptimizeResultDto(false, "사용 가능한 경로가 없습니다.", optimizeRule, 0.0, 0, 0.0, List.of());
        }

        List<RouteStepDto> steps = new ArrayList<>();
        double totalDistance = 0.0;
        int totalTime = 0;
        double totalCost = 0.0;

        for (int i = 0; i < path.size(); i++) {
            RouteLeg leg = path.get(i);
            RouteEdgeDto edge = leg.edge();
            totalDistance += defaultDouble(edge.distanceM());
            totalTime += defaultInt(edge.travelTimeSec());
            totalCost += leg.cost();
            steps.add(new RouteStepDto(
                    null,
                    null,
                    i + 1,
                    edge.routeEdgeId(),
                    leg.fromNodeId(),
                    leg.toNodeId(),
                    "WAITING",
                    edge.travelTimeSec(),
                    edge.edgeCd(),
                    leg.fromNodeCd() + " -> " + leg.toNodeCd(),
                    leg.fromNodeCd(),
                    leg.fromNodeNm(),
                    leg.toNodeCd(),
                    leg.toNodeNm(),
                    edge.edgeStatus()
            ));
        }

        return new RouteOptimizeResultDto(true, "경로 계산이 완료되었습니다.", optimizeRule, totalDistance, totalTime, totalCost, steps);
    }

    public Optional<TransferRouteDto> getTransferRoute(Long transferId) {
        return routeMapper.selectTransferRoute(transferId)
                .map(route -> new TransferRouteDto(
                        route.transferRouteId(),
                        route.transferId(),
                        route.routeStatus(),
                        route.totalDistanceM(),
                        route.totalTimeSec(),
                        route.totalCost(),
                        route.optimizeRule(),
                        route.replanCount(),
                        route.regUserId(),
                        route.regDtm(),
                        route.updUserId(),
                        route.updDtm(),
                        routeMapper.selectTransferRouteSteps(route.transferRouteId())
                ));
    }

    public boolean hasTransferRoute(Long transferId) {
        return routeMapper.selectTransferRoute(transferId).isPresent();
    }

    @Transactional
    public TransferRouteDto createRouteForTransfer(Long transferId, String optimizeRule, String userId) {
        TransferOrderDto transfer = transferMapper.selectTransferById(transferId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSFER_NOT_FOUND));

        RouteOptimizeResultDto result = optimize(new RouteOptimizeRequest(
                transfer.plantCd(),
                transfer.fromLocationId(),
                transfer.toLocationId(),
                optimizeRule
        ));

        if (!Boolean.TRUE.equals(result.routeAvailable())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, result.message());
        }

        routeMapper.deleteTransferRouteStepsByTransferId(transferId);
        routeMapper.deleteTransferRouteByTransferId(transferId);

        TransferRouteDto route = new TransferRouteDto(
                null,
                transferId,
                "PLANNED",
                result.totalDistanceM(),
                result.totalTimeSec(),
                result.totalCost(),
                result.optimizeRule(),
                0,
                userId,
                null,
                null,
                null,
                null
        );
        routeMapper.insertTransferRoute(route);
        TransferRouteDto savedRoute = routeMapper.selectTransferRoute(transferId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));

        for (RouteStepDto step : result.steps()) {
            routeMapper.insertTransferRouteStep(new RouteStepDto(
                    null,
                    savedRoute.transferRouteId(),
                    step.stepSeq(),
                    step.routeEdgeId(),
                    step.fromNodeId(),
                    step.toNodeId(),
                    "WAITING",
                    step.expectedTimeSec(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            ));
        }

        return getTransferRoute(transferId).orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
    }

    @Transactional
    public TransferRouteDto ensureRouteForTransfer(Long transferId, String optimizeRule, String userId) {
        return getTransferRoute(transferId).orElseGet(() -> createRouteForTransfer(transferId, optimizeRule, userId));
    }

    @Transactional
    public void changeTransferRouteStatus(Long transferId, String routeStatus, String userId) {
        if (hasTransferRoute(transferId)) {
            routeMapper.updateTransferRouteStatus(transferId, routeStatus, userId);
        }
    }

    @Transactional
    public void deleteTransferRoute(Long transferId) {
        routeMapper.deleteTransferRouteStepsByTransferId(transferId);
        routeMapper.deleteTransferRouteByTransferId(transferId);
    }

    @Transactional
    public void changeEdgeStatus(Long routeEdgeId, String edgeStatus, String userId) {
        if (routeEdgeId == null || edgeStatus == null || edgeStatus.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        routeMapper.updateRouteEdgeStatus(routeEdgeId, edgeStatus, userId);
    }

    private void validateOptimizeRequest(RouteOptimizeRequest request) {
        if (request.plantCd() == null || request.plantCd().isBlank()
                || request.fromLocationId() == null
                || request.toLocationId() == null
                || request.fromLocationId().equals(request.toLocationId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private String normalizeOptimizeRule(String optimizeRule) {
        if (optimizeRule == null || optimizeRule.isBlank()) {
            return "SHORTEST_TIME";
        }
        return optimizeRule;
    }

    private Map<Long, List<RouteLeg>> buildGraph(List<RouteEdgeDto> edges, String optimizeRule) {
        Map<Long, List<RouteLeg>> graph = new HashMap<>();
        for (RouteEdgeDto edge : edges) {
            addLeg(graph, edge, edge.fromNodeId(), edge.toNodeId(), edge.fromNodeCd(), edge.fromNodeNm(), edge.toNodeCd(), edge.toNodeNm(), optimizeRule);
            if ("Y".equals(edge.bidirectionalYn())) {
                addLeg(graph, edge, edge.toNodeId(), edge.fromNodeId(), edge.toNodeCd(), edge.toNodeNm(), edge.fromNodeCd(), edge.fromNodeNm(), optimizeRule);
            }
        }
        return graph;
    }

    private void addLeg(
            Map<Long, List<RouteLeg>> graph,
            RouteEdgeDto edge,
            Long fromNodeId,
            Long toNodeId,
            String fromNodeCd,
            String fromNodeNm,
            String toNodeCd,
            String toNodeNm,
            String optimizeRule
    ) {
        graph.computeIfAbsent(fromNodeId, ignored -> new ArrayList<>())
                .add(new RouteLeg(edge, fromNodeId, toNodeId, fromNodeCd, fromNodeNm, toNodeCd, toNodeNm, calculateCost(edge, optimizeRule)));
    }

    private List<RouteLeg> findShortestPath(Long startNodeId, Long endNodeId, Map<Long, List<RouteLeg>> graph) {
        Map<Long, Double> distances = new HashMap<>();
        Map<Long, RouteLeg> previous = new HashMap<>();
        PriorityQueue<RouteDistance> queue = new PriorityQueue<>(Comparator.comparingDouble(RouteDistance::distance));
        Set<Long> visited = new HashSet<>();

        distances.put(startNodeId, 0.0);
        queue.add(new RouteDistance(startNodeId, 0.0));

        while (!queue.isEmpty()) {
            RouteDistance current = queue.poll();
            if (!visited.add(current.nodeId())) {
                continue;
            }
            if (current.nodeId().equals(endNodeId)) {
                break;
            }

            for (RouteLeg leg : graph.getOrDefault(current.nodeId(), List.of())) {
                double candidate = distances.get(current.nodeId()) + leg.cost();
                if (candidate < distances.getOrDefault(leg.toNodeId(), Double.MAX_VALUE)) {
                    distances.put(leg.toNodeId(), candidate);
                    previous.put(leg.toNodeId(), leg);
                    queue.add(new RouteDistance(leg.toNodeId(), candidate));
                }
            }
        }

        if (!previous.containsKey(endNodeId)) {
            return List.of();
        }

        ArrayDeque<RouteLeg> path = new ArrayDeque<>();
        Long nodeId = endNodeId;
        while (!nodeId.equals(startNodeId)) {
            RouteLeg leg = previous.get(nodeId);
            if (leg == null) {
                return List.of();
            }
            path.addFirst(leg);
            nodeId = leg.fromNodeId();
        }
        return new ArrayList<>(path);
    }

    private double calculateCost(RouteEdgeDto edge, String optimizeRule) {
        double cost = switch (optimizeRule) {
            case "SHORTEST_DISTANCE" -> defaultDouble(edge.distanceM());
            case "AVOID_CONGESTION" -> defaultDouble(edge.baseCost()) + congestionPenalty(edge);
            default -> defaultInt(edge.travelTimeSec());
        };
        return cost <= 0 ? 1 : cost;
    }

    private double congestionPenalty(RouteEdgeDto edge) {
        return "CONGESTED".equals(edge.edgeStatus()) ? CONGESTION_AVOIDANCE_PENALTY : 0.0;
    }

    private double defaultDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private record RouteLeg(
            RouteEdgeDto edge,
            Long fromNodeId,
            Long toNodeId,
            String fromNodeCd,
            String fromNodeNm,
            String toNodeCd,
            String toNodeNm,
            double cost
    ) {}

    private record RouteDistance(Long nodeId, double distance) {}
}

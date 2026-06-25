package com.mes.mcs.domain.plc.event;

/**
 * PLC 이벤트 저장/처리가 끝났음을 알리는 내부 도메인 이벤트.
 *
 * <p>
 * 알림 리스너는 이 eventId로 PLC 이벤트 상세를 다시 조회해 알림 생성 여부를 판단한다.
 * </p>
 */
public record PlcEventProcessedEvent(Long eventId) {
}

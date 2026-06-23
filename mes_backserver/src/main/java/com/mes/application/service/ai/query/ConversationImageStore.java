package com.mes.application.service.ai.query;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 챗봇 대화별 첨부 이미지를 "대화 동안만" 들고 있는 메모리 보관소.
 * 만료(1시간)·동시성 처리는 Caffeine 캐시가 담당하고, 이 클래스는 그 위에
 * "대화당 최근 5장" 규칙만 얹은 얇은 래퍼다. (디스크/DB 저장 없음)
 */
@Component
public class ConversationImageStore {

    /** 보관 단위: 이미지 바이트 + 형식(png/jpeg 등) + 저장 시각 */
    @Getter
    @AllArgsConstructor
    public static class HeldImage {
        private final byte[] data; // 이미지 원본 바이트
        private final MimeType mimeType; // image/png, image/jpeg 등
        private final Instant at; // 보관 시각
    }

    // 대화ID → 그 대화의 이미지 목록.
    // expireAfterAccess(1시간): 1시간 동안 조회/추가가 없으면 그 대화 항목을 자동 폐기(TTL).
    // maximumSize(500): 전체 대화 수 상한(메모리 폭주 방지).
    private final Cache<String, List<HeldImage>> cache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .maximumSize(500)
            .build();

    /** 이미지 1장 보관. 6장째가 되면 가장 오래된 것을 빼서 항상 최대 5장 유지. */
    public void add(String convId, HeldImage img) {
        // 같은 대화에 동시 추가가 겹쳐도 안전하도록 synchronizedList 사용
        List<HeldImage> list = cache.get(convId, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (list) {
            list.add(img);
            if (list.size() > 5) { // 5장 상한
                list.remove(0); // 가장 오래된 것 제거
            }
        }
    }

    /** 해당 대화의 보관 이미지 목록 반환 (없으면 빈 목록). 모델 요청에 첨부할 때 사용. */
    public List<HeldImage> get(String convId) {
        List<HeldImage> list = cache.getIfPresent(convId);
        if (list == null) {
            return List.of();
        }
        synchronized (list) {
            return new ArrayList<>(list); // 복사본 반환 (바깥에서 안전하게 순회)
        }
    }

    /** 해당 대화의 이미지 전체 삭제 (새 대화 시작 / 대화 비움 때 호출). */
    public void clear(String convId) {
        cache.invalidate(convId);
    }
}
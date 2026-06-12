package com.mes.infra.persistence.mybatis.mapper.ai;

import com.mes.domain.ai.dto.AiNotificationDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiNotificationMapper {

    void insert(AiNotificationDto dto);

    List<AiNotificationDto> findRecent(@Param("limit") int limit);

    int countUnread();

    void markAsRead(@Param("id") Long id);

    void markAllAsRead();

    int countBySourceRef(@Param("sourceRef") String sourceRef);
}

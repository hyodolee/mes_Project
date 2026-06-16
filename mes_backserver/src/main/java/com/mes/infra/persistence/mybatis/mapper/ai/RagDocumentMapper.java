package com.mes.infra.persistence.mybatis.mapper.ai;

import com.mes.domain.ai.dto.RagDocumentDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RagDocumentMapper {

    void insert(RagDocumentDto dto);

    List<RagDocumentDto> findAll();

    RagDocumentDto findById(@Param("documentId") Long documentId);

    void updateStatus(
            @Param("documentId") Long documentId,
            @Param("status") String status,
            @Param("chunkCount") int chunkCount,
            @Param("errorMessage") String errorMessage
    );
}

package com.mes.infra.persistence.mybatis.mapper.auth;

import com.mes.domain.auth.dto.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    UserAccount findByUserId(@Param("userId") String userId);

    int countByUserId(@Param("userId") String userId);

    void insertUser(UserAccount user);
}

package com.mcs;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = "com.mcs.infra.persistence.mybatis.mapper")
public class McsApplication {

    public static void main(String[] args) {
        SpringApplication.run(McsApplication.class, args);
    }
}

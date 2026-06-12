package com.mes;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan(basePackages = "com.mes.infra.persistence.mybatis.mapper")
public class MesBackserverApplication {

    public static void main(String[] args) {
        SpringApplication.run(MesBackserverApplication.class, args);
    }
}

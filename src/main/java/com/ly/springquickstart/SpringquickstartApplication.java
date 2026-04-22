package com.ly.springquickstart;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.ly.springquickstart.mapper")
public class SpringquickstartApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringquickstartApplication.class, args);
    }

}

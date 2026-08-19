package com.ledger;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 个人云端记账本启动类
 */
@SpringBootApplication
@MapperScan("com.ledger.modules.**.mapper")
@EnableAsync
@EnableScheduling
public class LedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerApplication.class, args);
    }
}

package com.shiwei.seckill;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.shiwei.seckill.**.mapper")
@SpringBootApplication
@EnableScheduling
public class ShiweiSeckillApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShiweiSeckillApplication.class, args);
    }
}


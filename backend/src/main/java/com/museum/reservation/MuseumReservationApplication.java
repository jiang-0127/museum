package com.museum.reservation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@MapperScan("com.museum.reservation.module")
@SpringBootApplication
public class MuseumReservationApplication {

    public static void main(String[] args) {
        SpringApplication.run(MuseumReservationApplication.class, args);
    }
}

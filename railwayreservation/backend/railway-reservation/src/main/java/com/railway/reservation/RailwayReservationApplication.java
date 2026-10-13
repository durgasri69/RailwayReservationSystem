package com.railway.reservation;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import com.railway.reservation.cli.RailwayCLI;

@SpringBootApplication
public class RailwayReservationApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context =
                new SpringApplicationBuilder(RailwayReservationApplication.class)
                        .web(WebApplicationType.NONE)
                        .run(args);

        context.getBean(RailwayCLI.class).run();
        context.close();
    }
}

package at.htlle.reserveAndPreorderCookG2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ReserveAndPreorderCookG2Application {

    public static void main(String[] args) {
        SpringApplication.run(ReserveAndPreorderCookG2Application.class, args);
    }

}

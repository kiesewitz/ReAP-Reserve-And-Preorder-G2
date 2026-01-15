package at.htlle.reap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ReserveAndPreorderChefG2Application {

	public static void main(String[] args) {
		SpringApplication.run(ReserveAndPreorderChefG2Application.class, args);
	}

}

package com.terra.numerica.steiner_tree_solver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
public class SteinerTreeSolverApplication {

	public static void main(String[] args) {
		SpringApplication.run(SteinerTreeSolverApplication.class, args);
	}

}

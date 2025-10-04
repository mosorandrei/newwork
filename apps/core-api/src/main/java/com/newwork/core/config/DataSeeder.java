package com.newwork.core.config;

import com.newwork.core.domain.Employee;
import com.newwork.core.repo.EmployeeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {
    @Bean CommandLineRunner seed(EmployeeRepository repo) {
        return args -> {
            if (repo.count() == 0) {
                var e = new Employee();
                e.setFirstName("Alice");
                e.setLastName("Ng");
                repo.save(e);
            }
        };
    }
}

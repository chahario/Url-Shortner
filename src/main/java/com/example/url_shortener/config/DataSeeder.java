package com.example.url_shortener.config;

import com.example.url_shortener.domain.Plan;
import com.example.url_shortener.repository.PlanRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.example.url_shortener.domain.Client;
import com.example.url_shortener.repository.ClientRepository;

import java.math.BigDecimal;

@Component
public class DataSeeder implements CommandLineRunner {

    private final PlanRepository planRepository;
    private final ClientRepository clientRepository;

    public DataSeeder(PlanRepository planRepository,
                      ClientRepository clientRepository) {
        this.planRepository = planRepository;
        this.clientRepository = clientRepository;
    }

    @Override
    public void run(String... args) {
        seedPlan("free", new BigDecimal("0.0000"), 10);
        seedPlan("pro",  new BigDecimal("0.0050"), 1000);
        seedClient();
    }
    private void seedClient() {
        String demoKey = "demo-api-key-12345";
        if (clientRepository.findByApiKey(demoKey).isEmpty()) {
            Plan pro = planRepository.findByName("pro").orElseThrow();
            clientRepository.save(new Client("Demo Client", "demo@example.com", demoKey, pro));
        }
    }


    private void seedPlan(String name, BigDecimal rate, int rateLimit) {
        if (planRepository.findByName(name).isEmpty()) {
            planRepository.save(new Plan(name, rate, rateLimit));
        }
    }
}
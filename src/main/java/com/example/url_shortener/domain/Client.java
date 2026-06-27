package com.example.url_shortener.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "client")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "api_key", nullable = false, unique = true)
    private String apiKey;                  // authenticates this client's API calls

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;                      // which pricing tier this client is on

    @Column(name = "status", nullable = false)
    private String status = "active";       // active / suspended

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Client() {
    }

    public Client(String name, String email, String apiKey, Plan plan) {
        this.name = name;
        this.email = email;
        this.apiKey = apiKey;
        this.plan = plan;
        this.status = "active";
        this.createdAt = Instant.now();
    }

    public Long getClientId()  { return clientId; }
    public String getName()    { return name; }
    public String getEmail()   { return email; }
    public String getApiKey()  { return apiKey; }
    public Plan getPlan()      { return plan; }
    public String getStatus()  { return status; }
}
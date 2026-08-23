package com.eventpulse.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "monitored_endpoints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonitoredEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String url;

    @Builder.Default
    @Column(nullable = false)
    private String httpMethod = "GET";

    @Builder.Default
    @Column(nullable = false)
    private Integer expectedStatusCode = 200;

    @Builder.Default
    @Column(nullable = false)
    private Integer checkIntervalSeconds = 60;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    private LocalDateTime lastCheckedAt;

    private String lastStatus; // e.g., "UP", "DOWN"

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "webhook_url")
    private String webhookUrl;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
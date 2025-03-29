package com.david.hlp.Spring.simulate.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "nginx_access_log")
public class NginxAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ip", nullable = false, length = 50)
    private String ip;

    @Column(name = "access_time", nullable = false, length = 50)
    private String accessTime;

    @Column(name = "method", nullable = false, length = 10)
    private String method;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "protocol", length = 20)
    private String protocol;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "bytes", length = 20)
    private String bytes;

    @Column(name = "referrer", columnDefinition = "TEXT")
    private String referrer;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    @Override
    public String toString() {
        return String.format(
            "NginxLog{ip='%s', time='%s', request='%s %s %s', status='%s', bytes='%s', referrer='%s', userAgent='%s', userId='%s'}", 
            ip,
            accessTime,
            method,
            path,
            protocol,
            status,
            bytes,
            referrer != null ? (referrer.length() > 30 ? referrer.substring(0, 27) + "..." : referrer) : "-", 
            userAgent != null ? (userAgent.length() > 30 ? userAgent.substring(0, 27) + "..." : userAgent) : "-",
            userId != null ? userId : "-"
        );
    }
}
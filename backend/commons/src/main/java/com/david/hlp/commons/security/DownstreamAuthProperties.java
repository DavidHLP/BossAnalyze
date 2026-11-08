package com.david.hlp.commons.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration for authenticating requests that reach a downstream service
 * directly (analytics or crawler) instead of through the primary API gateway.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "security.downstream-auth")
public class DownstreamAuthProperties {

    /** Enables the filter. The primary API must leave this disabled. */
    private boolean enabled;

    /** Authenticated endpoint returning the caller's live permission grants. */
    private String verificationUrl = "http://127.0.0.1:8080/api/auth/getUserPrivateInformation";

    /** Authenticated endpoint resolving whether a route path is permitted. */
    private String permissionCheckUrl = "http://127.0.0.1:8080/api/auth/hasPagePermission";

    private int connectTimeoutMs = 1000;

    private int readTimeoutMs = 2000;

    /** Request path prefixes that do not require a bearer token. */
    private List<String> publicPaths = new ArrayList<>(List.of("/actuator", "/error"));

    /** Request path prefixes that additionally require the dashboard grant. */
    private List<String> permissionProtectedPaths = new ArrayList<>();

    /** Page path checked through the primary API's permission endpoint. */
    private String dashboardPath = "/console/dashboard";

    /** Exact request path of the manual analysis trigger. */
    private String triggerPath = "";

    /** Grant required in addition to the dashboard grant for the trigger. */
    private String triggerPermission = "logs.analysis.trigger";
}

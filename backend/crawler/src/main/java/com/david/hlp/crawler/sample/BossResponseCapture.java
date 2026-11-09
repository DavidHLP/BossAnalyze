package com.david.hlp.crawler.sample;

import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;
import java.time.Instant;
import java.util.IdentityHashMap;
import java.util.Map;

/** A response belongs to the action that STARTED its request, never the last card clicked. */
final class BossResponseCapture {
    private final Map<Request, Long> requests = new IdentityHashMap<>();
    private long generation;
    private SourceUrl target;
    Map<String, Object> result;
    String gate;
    boolean detailRequested;
    String failure = "detail_response_timeout";

    long begin(SourceUrl target) {
        this.target = target;
        result = null;
        detailRequested = false;
        failure = "detail_response_timeout";
        requests.clear();
        return ++generation;
    }
    SourceUrl target() { return target; }
    void started(Request request) {
        if (target != null && endpoint(request.url()) && "GET".equals(request.method())) {
            detailRequested = true;
            requests.put(request, generation);
        }
    }
    void observeGate(Response response) {
        try {
            var uri = SourceUrl.uri(response.url());
            if (SourceUrl.trusted(uri) && uri.getPath().startsWith("/wapi/") && "GET".equals(response.request().method())) {
                if (response.status() == 401) gate = "auth_required";
                if (response.status() == 403 || response.status() == 429) gate = "verification_required";
            }
        } catch (Exception ignored) { }

    }
    void received(Response response) {
        observeGate(response);
        Long action = requests.remove(response.request());
        if (action == null || action != generation || target == null || result != null) return;
        try {
            for (Request previous = response.request().redirectedFrom(); previous != null; previous = previous.redirectedFrom()) {
                if (!endpoint(previous.url()) || !"GET".equals(previous.method())) { failure = "redirect_rejected"; return; }
            }
            int status = response.status();
            if (status == 401) { failure = "auth_required"; return; }
            if (status == 403 || status == 429) { failure = "verification_required"; return; }
            if (!SourceUrl.detailResponse(response.url(), response.request().method(), status, response.headerValue("content-type"))) {
                failure = "invalid_detail_response";
                return;
            }
            accept(action, response.text());
        } catch (Exception exception) { failure = "detail_read_failed"; }
    }
    boolean accept(long action, String body) {
        if (action != generation || target == null || result != null) return false;
        if (body.length() > 2 * 1024 * 1024) { failure = "detail_too_large"; return false; }
        Map<String, Object> sample = DetailParser.parse(target, body, Instant.now().toString());
        if (!"ok".equals(sample.get("status"))) { failure = (String) sample.get("status"); return false; }
        result = sample;
        return true;
    }
    static boolean endpoint(String url) {
        try {
            var uri = SourceUrl.uri(url);
            return SourceUrl.trusted(uri) && "/wapi/zpgeek/job/detail.json".equals(uri.getRawPath());
        } catch (Exception exception) { return false; }
    }
}

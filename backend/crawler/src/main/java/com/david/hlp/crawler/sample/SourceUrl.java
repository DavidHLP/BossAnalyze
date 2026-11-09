package com.david.hlp.crawler.sample;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

/** BOSS navigation and response boundaries, independent of browser state. */
public record SourceUrl(URI uri, String jobId) {
    public static SourceUrl parse(String value) {
        URI uri = uri(value);
        if (!trusted(uri) || uri.getRawFragment() != null) throw new IllegalArgumentException("invalid_source_url");
        String path = uri.getRawPath();
        if (path.matches("/job_detail/[A-Za-z0-9_-]+\\.html") && uri.getRawQuery() == null)
            return new SourceUrl(uri, path.substring(12, path.length() - 5));
        if (!path.equals("/web/geek/jobs") && !path.equals("/web/geek/job"))
            throw new IllegalArgumentException("invalid_source_url");
        var keys = new HashSet<String>();
        if (uri.getRawQuery() != null) for (String part : uri.getRawQuery().split("&", -1)) {
            String[] pair = part.split("=", 2);
            if (pair.length != 2 || !keys.add(pair[0])) throw new IllegalArgumentException("invalid_search_query");
            String decoded;
            try { decoded = URLDecoder.decode(pair[1], StandardCharsets.UTF_8); }
            catch (Exception exception) { throw new IllegalArgumentException("invalid_search_query"); }
            if (!(pair[0].equals("query") && !decoded.isBlank() && decoded.length() <= 100
                    && decoded.chars().noneMatch(Character::isISOControl))
                    && !(pair[0].equals("city") && decoded.matches("[0-9]{3,12}")))
                throw new IllegalArgumentException("invalid_search_query");
        }
        return new SourceUrl(uri, null);
    }

    static SourceUrl cardLink(String value) {
        URI link = uri("https://www.zhipin.com").resolve(value);
        if (!trusted(link) || !link.getRawPath().matches("/job_detail/[A-Za-z0-9_-]+\\.html"))
            throw new IllegalArgumentException("invalid_card_link");
        // Use the path only as identity; never retain or replay signed card query parameters.
        return parse("https://www.zhipin.com" + link.getRawPath());
    }
    static URI uri(String value) {
        try { return URI.create(value); }
        catch (Exception exception) { throw new IllegalArgumentException("invalid_url"); }
    }
    static boolean trusted(URI uri) {
        return "https".equals(uri.getScheme()) && "www.zhipin.com".equals(uri.getHost())
                && uri.getPort() == -1 && uri.getRawUserInfo() == null;
    }
    static boolean detailResponse(String url, String method, int status, String contentType) {
        try {
            URI uri = uri(url);
            return trusted(uri) && uri.getRawFragment() == null
                    && "/wapi/zpgeek/job/detail.json".equals(uri.getRawPath())
                    && "GET".equals(method) && status == 200 && contentType != null
                    && contentType.split(";", 2)[0].strip().equalsIgnoreCase("application/json");
        } catch (Exception exception) { return false; }
    }
    static String cdp(String value) {
        URI uri = uri(value);
        if (!"http".equals(uri.getScheme()) || !("127.0.0.1".equals(uri.getHost()) || "[::1]".equals(uri.getHost()))
                || uri.getPort() < 1 || uri.getRawUserInfo() != null || uri.getRawQuery() != null
                || uri.getRawFragment() != null || !(uri.getRawPath().isEmpty() || uri.getRawPath().equals("/")))
            throw new IllegalArgumentException("invalid_cdp_endpoint");
        return uri.toString();
    }
    static SourceUrl detail(String id) { return parse("https://www.zhipin.com/job_detail/" + id + ".html"); }
}

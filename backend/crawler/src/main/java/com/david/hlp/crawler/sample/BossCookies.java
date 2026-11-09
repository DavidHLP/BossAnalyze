package com.david.hlp.crawler.sample;

import com.microsoft.playwright.options.Cookie;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class BossCookies {
    private BossCookies() { }
    static List<Cookie> read(Path file) throws Exception {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.size(file) > 131072
                || Files.getPosixFilePermissions(file).stream().anyMatch(p -> p.name().startsWith("GROUP_") || p.name().startsWith("OTHERS_")))
            throw new IllegalArgumentException("invalid_private_cookie_file");
        return parse(Files.readString(file));
    }
    static List<Cookie> parse(String json) {
        try {
            var root = DetailParser.JSON.readTree(json);
            if (root == null || !root.isArray() || root.isEmpty() || root.size() > 100)
                throw new IllegalArgumentException();
            List<Cookie> cookies = new ArrayList<>();
            for (var node : root) {
                String domain = DetailParser.scalar(node.get("domain"));
                String name = DetailParser.scalar(node.get("name"));
                String value = DetailParser.scalar(node.get("value"));
                if (!("www.zhipin.com".equals(domain) || ".zhipin.com".equals(domain) || "zhipin.com".equals(domain))
                        || name == null || value == null || !name.matches("[A-Za-z0-9_-]+")
                        || value.chars().anyMatch(Character::isISOControl)
                        || !"/".equals(DetailParser.scalar(node.get("path")))) throw new IllegalArgumentException();
                cookies.add(new Cookie(name, value).setDomain(domain).setPath("/").setSecure(true)
                        .setHttpOnly(node.path("httpOnly").asBoolean(false)));
            }
            return cookies;
        } catch (Exception exception) { throw new IllegalArgumentException("invalid_boss_cookies"); }
    }
}

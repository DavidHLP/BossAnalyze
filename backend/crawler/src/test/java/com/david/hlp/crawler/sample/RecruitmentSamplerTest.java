package com.david.hlp.crawler.sample;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.microsoft.playwright.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecruitmentSamplerTest {
    @TempDir Path temp;
    static final String URL = "https://www.zhipin.com/job_detail/synthetic_001.html";
    static final String API = "https://www.zhipin.com/wapi/zpgeek/job/detail.json?securityId=DO_NOT_LOG";
    private String fixture() throws Exception {
        try (var input = getClass().getResourceAsStream("/sample/boss-detail.json")) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test void extractsOnlyJobCompanyFactsAndPreservesRawJdAndNullGold() throws Exception {
        var sample = DetailParser.parse(SourceUrl.parse(URL), fixture(), "now");
        assertEquals("ok", sample.get("status"));
        var facts = (Map<?, ?>) sample.get("facts");
        assertEquals("合成 Java 工程师", facts.get("title"));
        assertEquals("合成公司", facts.get("company"));
        assertEquals("北京", facts.get("city"));
        assertEquals("20-30K", facts.get("salary"));
        assertEquals("3-5年", facts.get("experience"));
        assertEquals("本科", facts.get("education"));
        assertEquals("合成岗位（不是线上数据）。\n职责：Java 17；Unicode 😀。\n要求：保留原文与换行。", facts.get("rawJd"));
        assertNull(facts.get("rawBenefits"));
        var detail = (Map<?, ?>) sample.get("detailData");
        assertEquals(facts.get("rawJd"), ((Map<?, ?>) detail.get("jobDescription")).get("fullDescription"));
        assertNull(detail.get("jobTags"));
        var labels = DetailParser.labels(sample);
        assertEquals("pending", labels.get("status"));
        for (String key : List.of("requirements", "benefits", "evidenceSpans", "ranking", "relevance", "annotator")) assertNull(labels.get(key));
        assertEquals("blind_holdout", sample.get("split"));
        String exported = DetailParser.toJson(sample) + DetailParser.toJson(labels);
        for (String forbidden : List.of("NEVER_EXPORT", "bossInfo", "encryptUserId", "securityId", "NOT_WELFARE"))
            assertFalse(exported.contains(forbidden));
    }

    @Test void rejectsUntrustedNavigationResponseAndCdpBoundaries() {
        for (String url : List.of("http://www.zhipin.com/web/geek/jobs", "https://www.zhipin.com.evil.test/web/geek/jobs",
                "https://user:secret@www.zhipin.com/web/geek/jobs", "https://www.zhipin.com:443/web/geek/jobs",
                URL + "?token=private", "https://www.zhipin.com/web/geek/jobs?securityId=private",
                "https://www.zhipin.com/web/geek/jobs?query=a&query=b", "https://www.zhipin.com/web/geek/jobs?city=abc",
                "https://127.0.0.1/web/geek/jobs", "file:///tmp/page", "https://www.zhipin.com/job_detail/%xx.html"))
            assertThrows(IllegalArgumentException.class, () -> SourceUrl.parse(url));
        assertNull(SourceUrl.parse("https://www.zhipin.com/web/geek/jobs?query=Java&city=101010100").jobId());
        assertEquals("synthetic_001", SourceUrl.cardLink("/job_detail/synthetic_001.html?lid=not-exported").jobId());
        assertThrows(IllegalArgumentException.class, () -> SourceUrl.cardLink("https://evil.test/job_detail/a.html"));
        assertTrue(SourceUrl.detailResponse(API, "GET", 200, "application/json;charset=utf-8"));
        assertFalse(SourceUrl.detailResponse(API, "POST", 200, "application/json"));
        assertFalse(SourceUrl.detailResponse(API, "GET", 302, "application/json"));
        assertFalse(SourceUrl.detailResponse(API, "GET", 200, "text/html"));
        assertFalse(SourceUrl.detailResponse(API.replace("www.zhipin.com", "evil.test"), "GET", 200, "application/json"));
        assertFalse(SourceUrl.detailResponse(API.replace("detail.json", "detail.json.evil"), "GET", 200, "application/json"));
        assertEquals("http://127.0.0.1:9222", SourceUrl.cdp("http://127.0.0.1:9222"));
        for (String cdp : List.of("http://evil.test:9222", "http://127.0.0.1:9222?token=x", "http://user:secret@127.0.0.1:9222", "http://127.0.0.1:9222/json"))
            assertThrows(IllegalArgumentException.class, () -> SourceUrl.cdp(cdp));
    }

    @Test void invalidAndIncompleteDetailsNeverBecomeSamples() throws Exception {
        var source = SourceUrl.parse(URL);
        assertEquals("invalid_detail_json", DetailParser.parse(source, "{broken", "now").get("status"));
        assertEquals("api_error", DetailParser.parse(source, fixture().replace("\"code\": 0", "\"code\": 7"), "now").get("status"));
        assertEquals("api_error", DetailParser.parse(source, "{}", "now").get("status"));
        assertEquals("identity_mismatch", DetailParser.parse(SourceUrl.detail("other"), fixture(), "now").get("status"));
        var root = DetailParser.JSON.readTree(fixture());
        ((com.fasterxml.jackson.databind.node.ObjectNode) root.path("zpData").path("jobInfo")).put("postDescription", "  ");
        var failed = DetailParser.parse(source, root.toString(), "now");
        assertEquals("missing_detail", failed.get("status"));
        assertEquals(false, failed.get("detailComplete"));
        assertFalse(failed.containsKey("facts"));
    }

    @Test void staleOrMismatchedResponsesCannotOverwriteCurrentTarget() throws Exception {
        var capture = new BossResponseCapture();
        long old = capture.begin(SourceUrl.detail("other"));
        long current = capture.begin(SourceUrl.parse(URL));
        assertFalse(capture.accept(old, fixture()));
        assertNull(capture.result);
        assertFalse(capture.accept(current, fixture().replace("synthetic_001", "wrong")));
        assertEquals("identity_mismatch", capture.failure);
        assertTrue(capture.accept(current, fixture()));
        assertEquals("synthetic_001", capture.result.get("sourceJobId"));
        assertFalse(capture.accept(old, fixture()));
        Request request = mock(Request.class);
        when(request.url()).thenReturn(API);
        when(request.method()).thenReturn("GET");
        capture.begin(SourceUrl.parse(URL));
        capture.started(request);
        capture.begin(SourceUrl.parse(URL));
        Response late = mock(Response.class);
        when(late.request()).thenReturn(request);
        capture.received(late);
        verify(late, never()).text();
        assertNull(capture.result);
    }

    @Test void captureRejectsRedirectsAndStopsOn429WithoutReadingBody() {
        var capture = new BossResponseCapture();
        capture.begin(SourceUrl.parse(URL));
        Request request = mock(Request.class);
        when(request.url()).thenReturn(API);
        when(request.method()).thenReturn("GET");
        capture.started(request);
        Request previous = mock(Request.class);
        when(previous.url()).thenReturn("https://evil.test/detail");
        when(request.redirectedFrom()).thenReturn(previous);
        Response response = mock(Response.class);
        when(response.request()).thenReturn(request);
        when(response.url()).thenReturn(API);
        when(response.status()).thenReturn(200);
        capture.received(response);
        assertEquals("redirect_rejected", capture.failure);
        verify(response, never()).text();
        when(response.status()).thenReturn(429);
        capture.received(response);
        assertEquals("verification_required", capture.gate);
        verify(response, never()).text();
    }

    @Test void privateExportsDeduplicatePreserveRevisionsAndNeverOverwrite() throws Exception {
        Path directory = temp.resolve("private");
        var source = SourceUrl.parse(URL);
        var a = DetailParser.parse(source, fixture(), "first");
        var repeat = DetailParser.parse(source, fixture(), "second");
        var revision = DetailParser.parse(source, fixture().replace("Java 17", "Java 21"), "third");
        assertEquals(a.get("identity"), revision.get("identity"));
        assertEquals(a.get("revision"), repeat.get("revision"));
        assertNotEquals(a.get("revision"), revision.get("revision"));
        assertEquals(a.get("split"), revision.get("split"));
        try (var output = new RecruitmentSampler.PrivateOutput(directory)) {
            output.write(a); output.write(repeat); output.write(revision);
            output.write(DetailParser.failed(DetailParser.base(source, "now"), "auth_required"));
        }
        assertEquals(PosixFilePermissions.fromString("rwx------"), Files.getPosixFilePermissions(directory));
        assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(directory.resolve("samples.jsonl")));
        assertEquals(3, Files.readAllLines(directory.resolve("samples.jsonl")).size());
        assertEquals(2, Files.readAllLines(directory.resolve("labels.jsonl")).size());
        assertThrows(Exception.class, () -> new RecruitmentSampler.PrivateOutput(directory));
    }

    @Test void cookieFilesAreExplicitPrivateBossOnlyAndErrorsDoNotLeakValues() throws Exception {
        String cookie = "[{\"name\":\"synthetic\",\"value\":\"SYNTHETIC_SECRET\",\"domain\":\".zhipin.com\",\"path\":\"/\"}]";
        assertEquals(1, BossCookies.parse(cookie).size());
        var rejected = assertThrows(IllegalArgumentException.class, () -> BossCookies.parse(cookie.replace(".zhipin.com", ".evil.test")));
        assertFalse(rejected.toString().contains("SYNTHETIC_SECRET"));
        Path file = temp.resolve("cookie.json");
        Files.writeString(file, cookie);
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-r--r--"));
        assertThrows(IllegalArgumentException.class, () -> BossCookies.read(file));
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        assertEquals(1, BossCookies.read(file).size());
        Path link = temp.resolve("link"); Files.createSymbolicLink(link, file);
        assertThrows(IllegalArgumentException.class, () -> BossCookies.read(link));
    }
    @Test void cancellationAndClosureStopAndAttachedBrowserIsNeverClosed() throws Exception {
        Page page = mock(Page.class);
        Browser browser = mock(Browser.class);
        Path cancel = temp.resolve("cancel");
        var options = RecruitmentSampler.Options.parse(new String[]{"--url", URL, "--output", temp.resolve("out").toString(),
                "--cancel-file", cancel.toString(), "--limit", "1"});
        Files.createFile(cancel);
        assertEquals("cancelled", assertThrows(RecruitmentSampler.Stop.class,
                () -> RecruitmentSampler.check(page, options, Long.MAX_VALUE, false)).status);
        Files.delete(cancel);
        when(page.isClosed()).thenReturn(true);
        assertEquals("window_closed", assertThrows(RecruitmentSampler.Stop.class,
                () -> RecruitmentSampler.check(page, options, Long.MAX_VALUE, false)).status);
        when(page.isClosed()).thenReturn(false);
        assertEquals("time_limit", assertThrows(RecruitmentSampler.Stop.class,
                () -> RecruitmentSampler.check(page, options, 0, false)).status);
        RecruitmentSampler.closeOwned(page, browser, true);
        verify(page).close();
        verify(browser, never()).close();
        RecruitmentSampler.closeOwned(null, browser, false);
        verify(browser).close();
        assertThrows(IllegalArgumentException.class, () -> RecruitmentSampler.Options.parse(new String[]{"--url", URL,
                "--output", "out", "--limit", "21"}));
    }

    private RecruitmentSampler.Options options(String url, String name, Path cancel) {
        return RecruitmentSampler.Options.parse(new String[]{"--url", url, "--output", temp.resolve(name).toString(),
                "--cancel-file", cancel.toString(), "--limit", "1"});
    }

    private Page page(String url) {
        Page page = mock(Page.class);
        when(page.url()).thenReturn(url);
        var locators = new java.util.HashMap<String, Locator>();
        when(page.locator(anyString())).thenAnswer(call -> locators.computeIfAbsent(call.getArgument(0), key -> mock(Locator.class)));
        return page;
    }

    private String ssr() throws Exception {
        try (var input = getClass().getResourceAsStream("/sample/boss-detail.html")) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test void preCancelledRunNeverCreatesPlaywrightAndWritesCancelled() throws Exception {
        Path cancel = temp.resolve("cancel");
        Files.createFile(cancel);
        try (var playwright = mockStatic(Playwright.class)) {
            var options = options(URL, "cancel-run", cancel);
            assertEquals(2, RecruitmentSampler.run(options));
            playwright.verifyNoInteractions();
            assertTrue(Files.readString(options.output().resolve("samples.jsonl")).contains("cancelled"));
            Files.delete(cancel);
            Thread.currentThread().interrupt();
            try {
                assertEquals(2, RecruitmentSampler.run(options(URL, "interrupt-run", cancel)));
                playwright.verifyNoInteractions();
            } finally { Thread.interrupted(); }
        }
    }

    @Test void cancelledCollectAndManualLoginNeverNavigate() throws Exception {
        Path cancel = temp.resolve("cancel");
        Files.createFile(cancel);
        for (String url : List.of(URL, "https://www.zhipin.com/web/geek/jobs")) {
            Page page = mock(Page.class);
            var options = options(url, "cancel-collect", cancel);
            try (var output = new RecruitmentSampler.PrivateOutput(temp.resolve("out-" + (url.equals(URL) ? "detail" : "list")))) {
                assertEquals("cancelled", assertThrows(RecruitmentSampler.Stop.class,
                        () -> RecruitmentSampler.collect(page, new BossResponseCapture(), options, output)).status);
                assertEquals("cancelled", assertThrows(RecruitmentSampler.Stop.class,
                        () -> RecruitmentSampler.manualLogin(page, new BossResponseCapture(), options)).status);
                verifyNoInteractions(page);
            }
        }
    }

    @Test void switchWaitStopsBeforeTargetClickForGateCancelAndClosure() throws Exception {
        String list = "https://www.zhipin.com/web/geek/jobs";
        for (String stop : List.of("verification_required", "cancelled", "window_closed")) {
            Path cancel = temp.resolve("cancel-" + stop);
            Page page = page(list);
            var capture = new BossResponseCapture();
            Locator cards = mock(Locator.class), first = mock(Locator.class), second = mock(Locator.class);
            Locator links = mock(Locator.class), link = mock(Locator.class), body = mock(Locator.class);
            when(page.locator("li.job-card-box, li.job-card-wrapper")).thenReturn(cards);
            when(cards.count()).thenReturn(2);
            when(cards.nth(0)).thenReturn(first);
            when(cards.nth(1)).thenReturn(second);
            when(first.locator("a[href*='/job_detail/']")).thenReturn(links);
            when(links.count()).thenReturn(1);
            when(links.nth(0)).thenReturn(link);
            when(link.getAttribute("href")).thenReturn("/job_detail/synthetic_001.html");
            when(second.locator(".job-card-body")).thenReturn(body);
            when(body.count()).thenReturn(1);
            doAnswer(call -> {
                if (stop.equals("cancelled")) Files.createFile(cancel);
                else if (stop.equals("window_closed")) when(page.isClosed()).thenReturn(true);
                else {
                    Response response = mock(Response.class); Request request = mock(Request.class);
                    when(response.url()).thenReturn(API); when(response.request()).thenReturn(request);
                    when(request.method()).thenReturn("GET"); when(response.status()).thenReturn(429);
                    capture.observeGate(response);
                }
                return null;
            }).when(page).waitForTimeout(500);
            try (var output = new RecruitmentSampler.PrivateOutput(temp.resolve("switch-" + stop))) {
                assertEquals(stop, assertThrows(RecruitmentSampler.Stop.class,
                        () -> RecruitmentSampler.collect(page, capture, options(list, "unused", cancel), output)).status);
            }
            verify(body).click();
            verify(first, never()).locator(".job-card-body");
            verify(page, times(1)).navigate(eq(list), any(Page.NavigateOptions.class));
            verify(page, never()).mouse();
            assertNull(capture.target()); // No begin(target) after the switch wait.
        }
    }

    @Test void explicitNavigationReadsNoJsonSsrAndPreservesUnicodeAndMissingFields() throws Exception {
        Page page = page(URL);
        when(page.content()).thenReturn(ssr());
        doAnswer(call -> { Thread.sleep(100); return null; }).when(page).waitForTimeout(100);
        var options = options(URL, "ssr", temp.resolve("cancel"));
        try (var output = new RecruitmentSampler.PrivateOutput(options.output())) {
            assertEquals(0, RecruitmentSampler.collect(page, new BossResponseCapture(), options, output));
        }
        var sample = DetailParser.JSON.readTree(Files.readString(options.output().resolve("samples.jsonl")));
        assertEquals("boss_browser_detail_ssr", sample.path("samplingSource").asText());
        assertEquals("开发者 🧪", sample.path("facts").path("title").asText());
        assertEquals("第一行 Java 17\n第二行 Unicode 🧪\n第三行", sample.path("facts").path("rawJd").asText());
        for (String field : List.of("company", "city", "salary", "experience", "education", "rawBenefits"))
            assertTrue(sample.path("facts").path(field).isNull());
        assertTrue(sample.path("detailData").path("jobTags").isNull());
        assertFalse(sample.toString().contains("NOT_WELFARE"));
        assertTrue(Files.readString(options.output().resolve("labels.jsonl")).contains("pending"));
    }

    @Test void ssrRejectsEmptyJdWrongOriginIdentityAndLoginVerificationShells() throws Exception {
        var source = SourceUrl.parse(URL);
        assertEquals("missing_detail", DetailParser.parseSsr(source, URL, "<h1>Title</h1><div class='job-sec-text'> </div>", "now").get("status"));
        assertEquals("identity_mismatch", DetailParser.parseSsr(source, URL.replace("synthetic_001", "other"), ssr(), "now").get("status"));
        assertEquals("redirect_rejected", DetailParser.parseSsr(source, URL.replace("www.zhipin.com", "evil.test"), ssr(), "now").get("status"));
        assertEquals("auth_required", DetailParser.parseSsr(source, URL, ssr() + "<input type='password'>", "now").get("status"));
        assertEquals("verification_required", DetailParser.parseSsr(source, URL, ssr() + "<div id='captcha'></div>", "now").get("status"));
    }

    @Test void firstCardNavigationFallbackReadsSsrOnlyAfterNoJson() throws Exception {
        String list = "https://www.zhipin.com/web/geek/jobs";
        Page page = page(list);
        Locator cards = mock(Locator.class), card = mock(Locator.class), links = mock(Locator.class), body = mock(Locator.class);
        when(page.locator("li.job-card-box, li.job-card-wrapper")).thenReturn(cards);
        when(cards.count()).thenReturn(1); when(cards.nth(0)).thenReturn(card);
        when(card.locator("a[href*='/job_detail/']")).thenReturn(links);
        when(links.count()).thenReturn(1); when(links.nth(0)).thenReturn(links);
        when(links.getAttribute("href")).thenReturn("/job_detail/synthetic_001.html");
        when(card.locator(".job-card-body")).thenReturn(body); when(body.count()).thenReturn(1);
        doAnswer(call -> { when(page.url()).thenReturn(call.getArgument(0)); return null; })
                .when(page).navigate(anyString(), any(Page.NavigateOptions.class));
        doAnswer(call -> { Thread.sleep(100); return null; }).when(page).waitForTimeout(100);
        when(page.content()).thenReturn(ssr());
        var options = options(list, "fallback", temp.resolve("cancel"));
        try (var output = new RecruitmentSampler.PrivateOutput(options.output())) {
            assertEquals(0, RecruitmentSampler.collect(page, new BossResponseCapture(), options, output));
        }
        verify(body).click();
        verify(page).navigate(eq(URL), any(Page.NavigateOptions.class));
        verify(page, times(1)).content();
        String exported = Files.readString(options.output().resolve("samples.jsonl"));
        assertTrue(exported.contains("boss_browser_detail_ssr"));
        assertTrue(exported.contains("samplingListUrl"));
    }

    @Test void rejectedJsonIsNeverRescuedByReadableSsr() throws Exception {
        Page page = page(URL);
        var capture = new BossResponseCapture();
        doAnswer(call -> {
            capture.accept(1, fixture().replace("synthetic_001", "wrong"));
            return null;
        }).when(page).navigate(eq(URL), any(Page.NavigateOptions.class));
        doAnswer(call -> { Thread.sleep(100); return null; }).when(page).waitForTimeout(100);
        var options = options(URL, "bad-json", temp.resolve("cancel"));
        try (var output = new RecruitmentSampler.PrivateOutput(options.output())) {
            assertEquals(2, RecruitmentSampler.collect(page, capture, options, output));
        }
        verify(page, never()).content();
        assertTrue(Files.readString(options.output().resolve("samples.jsonl")).contains("identity_mismatch"));
    }

    @Test void noJsonNavigationCannotExportEmptyWrongIdentityForeignOrGatedSsr() throws Exception {
        for (String boundary : List.of("empty", "wrong", "foreign", "login", "verify")) {
            String current = boundary.equals("wrong") ? URL.replace("synthetic_001", "other")
                    : boundary.equals("foreign") ? URL.replace("www.zhipin.com", "evil.test") : URL;
            Page page = page(current);
            when(page.content()).thenReturn(boundary.equals("empty") ? "<h1>Title</h1><div class='job-sec-text'> </div>" : ssr());
            if (boundary.equals("login")) when(page.locator("input[type=password]:visible").count()).thenReturn(1);
            if (boundary.equals("verify")) when(page.locator("#captcha, .captcha, iframe[src*='captcha']").count()).thenReturn(1);
            doAnswer(call -> { Thread.sleep(100); return null; }).when(page).waitForTimeout(100);
            var options = options(URL, "boundary-" + boundary, temp.resolve("cancel"));
            try (var output = new RecruitmentSampler.PrivateOutput(options.output())) {
                if (List.of("foreign", "login", "verify").contains(boundary)) {
                    String status = boundary.equals("foreign") ? "redirect_rejected" : boundary.equals("login") ? "auth_required" : "verification_required";
                    assertEquals(status, assertThrows(RecruitmentSampler.Stop.class,
                            () -> RecruitmentSampler.collect(page, new BossResponseCapture(), options, output)).status);
                    verify(page, never()).content();
                } else assertEquals(2, RecruitmentSampler.collect(page, new BossResponseCapture(), options, output));
            }
            assertFalse(Files.readString(options.output().resolve("samples.jsonl")).contains("\"status\":\"ok\""));
            assertEquals("", Files.readString(options.output().resolve("labels.jsonl")));
        }
    }

}

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

}

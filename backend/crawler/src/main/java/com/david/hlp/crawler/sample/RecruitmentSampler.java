package com.david.hlp.crawler.sample;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** Explicit BOSS-only CLI. No Spring context, profile scanning, persistence or AI. */
public final class RecruitmentSampler {
    private static final String CARDS = "li.job-card-box, li.job-card-wrapper";
    private RecruitmentSampler() { }

    record Options(SourceUrl source, Path output, String cdp, Path cookies, int limit, Path cancel) {
        static Options parse(String[] args) {
            var values = new java.util.HashMap<String, String>();
            for (int i = 0; i < args.length; i += 2) {
                if (i + 1 == args.length || !List.of("--url", "--output", "--cdp", "--cookies", "--limit", "--cancel-file").contains(args[i])
                        || values.put(args[i], args[i + 1]) != null) throw new IllegalArgumentException("invalid_arguments");
            }
            if (!values.containsKey("--url") || !values.containsKey("--output")
                    || values.containsKey("--cdp") && values.containsKey("--cookies")) throw new IllegalArgumentException("invalid_arguments");
            int limit;
            try { limit = Integer.parseInt(values.getOrDefault("--limit", "5")); }
            catch (Exception exception) { throw new IllegalArgumentException("invalid_limit"); }
            if (limit < 1 || limit > 20) throw new IllegalArgumentException("invalid_limit");
            return new Options(SourceUrl.parse(values.get("--url")), Path.of(values.get("--output")),
                    values.containsKey("--cdp") ? SourceUrl.cdp(values.get("--cdp")) : null,
                    values.containsKey("--cookies") ? Path.of(values.get("--cookies")) : null, limit,
                    values.containsKey("--cancel-file") ? Path.of(values.get("--cancel-file")) : null);
        }
    }

    public static void main(String[] args) {
        int exit = 2;
        try { exit = run(Options.parse(args)); }
        catch (IllegalArgumentException exception) { System.err.println("BOSS sampler: invalid input or private cookie file; see docs/recruitment-sampling.md"); }
        catch (Exception exception) { System.err.println("BOSS sampler: browser or output unavailable; no authentication claim made."); }
        if (exit != 0) System.exit(exit);
    }

    static int run(Options options) throws Exception {
        try (PrivateOutput output = new PrivateOutput(options.output)) {
            Playwright playwright = null;
            boolean attached = options.cdp != null;
            Browser browser = null;
            Page page = null;
            BossResponseCapture capture = new BossResponseCapture();
            try {
                check(null, options, Long.MAX_VALUE, false);
                var cookies = options.cookies == null ? List.<com.microsoft.playwright.options.Cookie>of() : BossCookies.read(options.cookies);
                check(null, options, Long.MAX_VALUE, false);
                playwright = Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("DEBUG", "", "PWDEBUG", "0", "PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
                check(null, options, Long.MAX_VALUE, false);
                browser = attached ? playwright.chromium().connectOverCDP(options.cdp, new BrowserType.ConnectOverCDPOptions().setTimeout(15000))
                        : playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false).setTimeout(15000));
                check(null, options, Long.MAX_VALUE, false);
                BrowserContext context;
                if (attached) {
                    if (browser.contexts().isEmpty()) throw new IllegalStateException();
                    context = browser.contexts().get(0);
                } else {
                    context = browser.newContext();
                    if (!cookies.isEmpty()) context.addCookies(cookies);
                }
                check(null, options, Long.MAX_VALUE, false);
                page = context.newPage(); // Only this tool-owned tab may be navigated or closed.
                page.setDefaultTimeout(3000);
                page.setDefaultNavigationTimeout(15000);
                installNavigationBoundary(page, capture);
                page.onRequest(capture::started);
                page.onResponse(capture::observeGate);
                page.onRequestFinished(request -> {
                    Response response = request.response();
                    if (response != null) capture.received(response);
                });
                if (!attached && cookies.isEmpty()) manualLogin(page, capture, options);
                return collect(page, capture, options, output);
            } catch (Stop stop) {
                output.write(DetailParser.failed(DetailParser.base(options.source, Instant.now().toString()), stop.status));
                System.out.println("BOSS sampler stopped: " + stop.status);
                return 2;
            } catch (PlaywrightException exception) {
                String status = capture.gate == null ? "browser_unavailable" : capture.gate;
                output.write(DetailParser.failed(DetailParser.base(options.source, Instant.now().toString()), status));
                System.out.println("BOSS sampler stopped: " + status);
                return 2;
            } finally {
                try { closeOwned(page, browser, attached); }
                finally { if (playwright != null) playwright.close(); }
                // Playwright.close disconnects CDP; attached browsers and their other tabs are never closed.
            }
        }
    }

    static void closeOwned(Page page, Browser browser, boolean attached) {
        try { if (page != null && !page.isClosed()) page.close(); }
        finally { if (!attached && browser != null) browser.close(); }
    }
    private static void installNavigationBoundary(Page page, BossResponseCapture capture) {
        page.route("**/*", route -> {
            Request request = route.request();
            if (request.isNavigationRequest() && request.frame().equals(page.mainFrame())) {
                try {
                    var uri = SourceUrl.uri(request.url());
                    if (!SourceUrl.trusted(uri) || !"GET".equals(request.method())) { capture.gate = "redirect_rejected"; route.abort(); return; }
                    String path = uri.getPath();
                    if (!(path.equals("/") || path.startsWith("/web/user/") || (path.equals("/web/geek/job") || path.equals("/web/geek/jobs"))
                            || path.startsWith("/job_detail/"))) { capture.gate = "redirect_rejected"; route.abort(); return; }
                } catch (Exception exception) { capture.gate = "redirect_rejected"; route.abort(); return; }
            }
            route.resume();
        });
        page.onPopup(popup -> popup.close()); // Card reading must stay in the owned tab.
    }

    static void manualLogin(Page page, BossResponseCapture capture, Options options) throws Exception {
        ensureNoGate(capture);
        check(page, options, Long.MAX_VALUE, false);
        page.navigate("https://www.zhipin.com/", new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        System.out.println("Temporary BOSS window: complete manual login, then press Enter here within 120 seconds. Authentication will not be saved.");
        long deadline = System.nanoTime() + 120_000_000_000L;
        while (System.in.available() == 0) {
            ensureNoGate(capture);
            check(page, options, deadline, false);
            page.waitForTimeout(100);
        }
        while (System.in.available() > 0 && System.in.read() != '\n') { }
    }

    static int collect(Page page, BossResponseCapture capture, Options options, PrivateOutput output) {
        long deadline = System.nanoTime() + 120_000_000_000L;
        var seen = new HashSet<String>();
        int attempts = 0;
        int successes = 0;
        int scrolls = 0;
        int unchanged = 0;
        String discoveryStop = "no_new_cards";
        ensureNoGate(capture);
        check(page, options, deadline, false);
        if (options.source.jobId() != null) {
            capture.begin(options.source);
            page.navigate(options.source.uri().toString(), new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            Map<String, Object> sample = await(page, capture, options, deadline, true);
            outputUnchecked(output, sample);
            return "ok".equals(sample.get("status")) ? 0 : 2;
        }
        page.navigate(options.source.uri().toString(), new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        page.waitForTimeout(1000);
        while (attempts < options.limit) {
            ensureNoGate(capture);
            check(page, options, deadline, true);
            Locator cards = page.locator(CARDS);
            int before = seen.size();
            int count = Math.min(cards.count(), 100);
            for (int index = 0; index < count && attempts < options.limit; index++) {
                ensureNoGate(capture);
                check(page, options, deadline, true);
                // Re-query the DOM on every action; never retain a stale element handle.
                Locator card = page.locator(CARDS).nth(index);
                SourceUrl target = cardSource(card);
                if (target == null || !seen.add(target.jobId())) continue;
                capture.begin(target);
                // The default first selection may not issue a request: read another card then return.
                if (index == 0 && count > 1) {
                    capture.begin(null);
                    ensureNoGate(capture);
                    check(page, options, deadline, true);
                    readCard(page.locator(CARDS).nth(1), page, capture, options, deadline);
                    page.waitForTimeout(500);
                    ensureNoGate(capture);
                    check(page, options, deadline, true);
                    capture.begin(target);
                }
                ensureNoGate(capture);
                check(page, options, deadline, true);
                readCard(page.locator(CARDS).nth(index), page, capture, options, deadline);
                Map<String, Object> sample = await(page, capture, options, deadline, false);
                if (index == 0 && "detail_response_timeout".equals(sample.get("status"))) {
                    ensureNoGate(capture);
                    check(page, options, deadline, true);
                    capture.begin(target);
                    page.navigate(target.uri().toString(), new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                    sample = await(page, capture, options, deadline, true);
                }
                sample.put("samplingListUrl", options.source.uri().toString());
                outputUnchecked(output, sample);
                attempts++;
                if ("ok".equals(sample.get("status"))) successes++;
                if (!page.url().equals(options.source.uri().toString())) {
                    ensureNoGate(capture);
                    check(page, options, deadline, true);
                    page.navigate(options.source.uri().toString(), new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                    page.waitForTimeout(500);
                    break;
                }
                page.waitForTimeout(1000);
            }
            if (attempts >= options.limit) break;
            unchanged = seen.size() == before ? unchanged + 1 : 0;
            if (scrolls >= 5) { discoveryStop = "scroll_limit"; break; }
            if (unchanged >= 2) break;
            scrolls++;
            ensureNoGate(capture);
            check(page, options, deadline, true);
            page.mouse().wheel(0, 900);
            page.waitForTimeout(1000);
        }
        ensureNoGate(capture);
        check(page, options, deadline, true);
        String status = attempts == 0 ? "unsupported_layout" : attempts < options.limit ? discoveryStop : "limit_reached";
        var summary = DetailParser.failed(DetailParser.base(options.source, Instant.now().toString()), status);
        summary.put("recordType", "run_status");
        outputUnchecked(output, summary);
        System.out.println("BOSS sampler: " + attempts + " attempted, " + successes + " parsed; stop=" + status + ". Labels pending; login not asserted.");
        return successes > 0 && successes == attempts ? 0 : 2;
    }

    private static SourceUrl cardSource(Locator card) {
        Locator links = card.locator("a[href*='/job_detail/']");
        for (int i = 0; i < Math.min(links.count(), 5); i++) {
            try {
                String href = links.nth(i).getAttribute("href");
                var target = SourceUrl.cardLink(href);
                if (target.jobId() != null) return target;
            } catch (IllegalArgumentException exception) { /* Unsupported card identity is not a sample. */ }
        }
        return null;
    }
    private static void readCard(Locator card, Page page, BossResponseCapture capture, Options options, long deadline) {
        Locator body = card.locator(".job-card-body");
        if (body.count() != 1) throw new Stop("unsupported_layout");
        ensureNoGate(capture);
        check(page, options, deadline, true);
        body.click(); // Read area only; no button, recruiter or communication selector.
    }
    private static Map<String, Object> await(Page page, BossResponseCapture capture, Options options, long deadline, boolean detailNavigation) {
        long waitUntil = Math.min(deadline, System.nanoTime() + 8_000_000_000L);
        while (capture.result == null && System.nanoTime() < waitUntil) {
            ensureNoGate(capture);
            check(page, options, deadline, true);
            if (List.of("auth_required", "verification_required", "api_error").contains(capture.failure)) throw new Stop(capture.failure);
            page.waitForTimeout(100);
        }
        ensureNoGate(capture);
        check(page, options, deadline, true);
        if (capture.result != null) return capture.result;
        if (detailNavigation && !capture.detailRequested && "detail_response_timeout".equals(capture.failure)) {
            String currentUrl = page.url();
            String html = page.content();
            ensureNoGate(capture);
            check(page, options, deadline, true);
            if (!currentUrl.equals(page.url())) throw new Stop("redirect_rejected");
            var sample = DetailParser.parseSsr(capture.target(), currentUrl, html, Instant.now().toString());
            if (List.of("auth_required", "verification_required").contains(sample.get("status"))) throw new Stop((String) sample.get("status"));
            return sample;
        }
        return DetailParser.failed(DetailParser.base(capture.target(), Instant.now().toString()), capture.failure);
    }
    private static void ensureNoGate(BossResponseCapture capture) {
        if (capture.gate != null) throw new Stop(capture.gate);
    }
    static void check(Page page, Options options, long deadline, boolean detectGate) {
        if (Thread.currentThread().isInterrupted() || options.cancel != null && Files.exists(options.cancel)) throw new Stop("cancelled");
        if (page != null && page.isClosed()) throw new Stop("window_closed");
        if (System.nanoTime() >= deadline) throw new Stop("time_limit");
        if (!detectGate || page == null) return;
        var uri = SourceUrl.uri(page.url());
        if (!SourceUrl.trusted(uri)) throw new Stop("redirect_rejected");
        String path = uri.getPath();
        if (path.contains("verify") || path.contains("captcha") || page.locator("#captcha, .captcha, iframe[src*='captcha']").count() > 0)
            throw new Stop("verification_required");
        if (path.contains("login") || path.startsWith("/web/user/") || page.locator("input[type=password]:visible").count() > 0)
            throw new Stop("auth_required");
    }
    private static void outputUnchecked(PrivateOutput output, Map<String, Object> sample) {
        try { output.write(sample); } catch (Exception exception) { throw new Stop("output_failed"); }
    }
    static final class Stop extends RuntimeException {
        final String status;
        Stop(String status) { super(status); this.status = status; }
    }
    static final class PrivateOutput implements AutoCloseable {
        private final java.util.Set<String> seen = new HashSet<>();
        private final BufferedWriter samples;
        private final BufferedWriter labels;
        PrivateOutput(Path directory) throws Exception {
            Files.createDirectory(directory, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
            samples = writer(directory.resolve("samples.jsonl"));
            try { labels = writer(directory.resolve("labels.jsonl")); }
            catch (Exception exception) { samples.close(); throw exception; }
        }
        private static BufferedWriter writer(Path file) throws Exception {
            Files.createFile(file, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            return Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.WRITE);
        }
        void write(Map<String, Object> sample) throws Exception {
            if ("ok".equals(sample.get("status"))
                    && !seen.add(sample.get("identity") + ":" + sample.get("revision"))) return;
            samples.write(DetailParser.toJson(sample));
            samples.newLine();
            if ("ok".equals(sample.get("status"))) {
                labels.write(DetailParser.toJson(DetailParser.labels(sample)));
                labels.newLine();
            }
        }
        public void close() throws java.io.IOException {
            try { samples.close(); } finally { labels.close(); }
        }
    }
}

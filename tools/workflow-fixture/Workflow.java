package com.david.hlp.crawler.sample;

import java.nio.file.*;
import java.util.*;
import java.sql.*;
import java.net.*;
import java.net.http.*;
import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.*;
import com.microsoft.playwright.*;
import com.david.hlp.crawler.ai.service.*;
import com.david.hlp.crawler.ai.mapper.*;
import com.david.hlp.commons.recruitment.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.apache.ibatis.session.*;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;

/** Fixture assembly only. All sampling, model validation, transactions and DTO conversion use production classes. */
public class Workflow {
    static final ObjectMapper json = new ObjectMapper();
    static final Path out = Path.of(System.getenv("WORKFLOW_OUTPUT"));
    static final List<String> checks = new ArrayList<>();
    static void check(boolean value, String message) throws Exception {
        if (!value) throw new AssertionError(message);
        checks.add(message);
        json.writeValue(out.resolve("checks.json").toFile(), checks);
    }
    static void save(String name, Object value) throws Exception {
        json.writerWithDefaultPrettyPrinter().writeValue(out.resolve(name).toFile(), value);
    }
    static int calls() throws Exception { return json.readTree(out.resolve("provider-count.json").toFile()).path("calls").asInt(); }

    static void collect() throws Exception {
        var requests = new ArrayList<Map<String,String>>();
        JsonNode fixtures = json.readTree(Path.of("tools/workflow-fixture/boss.json").toFile());
        try (Playwright pw = Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
             Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setExecutablePath(Path.of("/usr/bin/chromium")).setHeadless(true));
             BrowserContext context = browser.newContext(new Browser.NewContextOptions().setServiceWorkers(com.microsoft.playwright.options.ServiceWorkerPolicy.BLOCK));
             RecruitmentSampler.PrivateOutput output = new RecruitmentSampler.PrivateOutput(out.resolve("sample"))) {
            Page page = context.newPage();
            page.setDefaultTimeout(3000);
            page.route("**/*", route -> {
                URI uri = URI.create(route.request().url());
                String path = uri.getRawPath();
                boolean trusted = "https".equals(uri.getScheme()) && "www.zhipin.com".equals(uri.getHost()) && uri.getPort() == -1 && "GET".equals(route.request().method());
                String body = null;
                String type = "text/html";
                if (trusted && path.equals("/web/geek/job") && uri.getRawQuery() == null) {
                    body = "<html><meta charset='utf-8'><ul>";
                    for (int i=1;i<=2;i++) body += "<li class='job-card-box'><a href='/job_detail/fixture"+i+".html'>合成岗位</a><div class='job-card-body' onclick=\"fetch('/wapi/zpgeek/job/detail.json?jobId=fixture"+i+"')\">合成读取区域 "+i+"</div></li>";
                    body += "</ul></html>";
                } else if (trusted && path.equals("/wapi/zpgeek/job/detail.json") && Set.of("jobId=fixture1","jobId=fixture2").contains(uri.getRawQuery())) {
                    body = fixtures.path(uri.getRawQuery().substring(6)).toString();
                    type = "application/json";
                }
                requests.add(Map.of("url", uri.toString(), "action", body == null ? "abort" : "fulfill"));
                if (body == null) route.abort();
                else route.fulfill(new Route.FulfillOptions().setStatus(200).setContentType(type).setBody(body));
            });
            var capture = new BossResponseCapture();
            page.onRequest(capture::started);
            page.onResponse(capture::observeGate);
            page.onRequestFinished(request -> { if (request.response()!=null) capture.received(request.response()); });
            var options = RecruitmentSampler.Options.parse(new String[]{"--url","https://www.zhipin.com/web/geek/job","--output",out.resolve("sample").toString(),"--limit","2"});
            check(RecruitmentSampler.collect(page, capture, options, output) == 0, "real collector succeeded for two synthetic list cards");
        } finally { save("browser-requests.json", requests); }
        check(requests.stream().allMatch(r -> r.get("action").equals("fulfill")), "every browser request fulfilled locally; no network resume");
    }

    public static void main(String[] args) throws Exception {
        collect();
        var samples = new ArrayList<JsonNode>();
        for (String line : Files.readAllLines(out.resolve("sample/samples.jsonl"))) {
            JsonNode sample = json.readTree(line);
            if (sample.path("status").asText().equals("ok")) samples.add(sample);
        }
        check(samples.size()==2, "PrivateOutput exported two actual samples");
        for (String line : Files.readAllLines(out.resolve("sample/labels.jsonl"))) check(json.readTree(line).path("status").asText().equals("pending"), "labels remain pending");
        String python = "http://127.0.0.1:" + Files.readString(out.resolve("python-port.txt"));
        int before = calls();
        var http = HttpClient.newHttpClient();
        var denied = http.send(HttpRequest.newBuilder(URI.create(python+"/internal/v1/job-analysis")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{}")).build(),HttpResponse.BodyHandlers.ofString());
        check(denied.statusCode()==401 && calls()==before, "HTTP missing token rejected before provider");
        save("negative-auth.json", Map.of("httpStatus", denied.statusCode(), "providerCallsBefore",before,"providerCallsAfter",calls()));
        var ai = new AIExtractionOfJobRequirements(json, python, "synthetic-internal", "typesafe/jev-1.14.0");
        Properties props = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("/home/david/.local/share/bossanalyze/local-dev/application-local.properties"))) { props.load(reader); }
        URI uri = URI.create(props.getProperty("spring.datasource.url", "jdbc:mysql://127.0.0.1:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai").substring(5));
        check(Set.of("localhost","127.0.0.1").contains(uri.getHost()), "configured database is loopback");
        String schema = "workflow_20261108_" + UUID.randomUUID().toString().replace("-", "").substring(0,12);
        String base = "jdbc:mysql://"+uri.getRawAuthority()+"/";
        String query = uri.getRawQuery()==null ? "" : "?"+uri.getRawQuery();
        String user=props.getProperty("spring.datasource.username"), password=props.getProperty("spring.datasource.password");
        try (var connection=DriverManager.getConnection(base+query,user,password)) { connection.createStatement().execute("CREATE DATABASE `"+schema+"` CHARACTER SET utf8mb4"); }
        Files.writeString(out.resolve("schema.txt"), schema);
        var ds = new DriverManagerDataSource(base+schema+query,user,password);
        var db = new JdbcTemplate(ds);
        db.execute("CREATE TABLE t_job_detail(id BIGINT PRIMARY KEY,detail_data JSON,is_deleted INT NOT NULL DEFAULT 0,job_requirements TEXT,employee_benefits TEXT,city_name VARCHAR(64),position_name VARCHAR(64),html_url TEXT)");
        db.execute(Files.readString(Path.of("database/migrations/20261108_job_ai_analysis.sql")));
        for (int i=1;i<=3;i++) {
            JsonNode sample=samples.get((i-1)%2);
            db.update("INSERT INTO t_job_detail VALUES(?,?,0,'[]','[]',?,?,?)",i,sample.path("detailData").toString(),sample.at("/facts/city").asText(),sample.at("/facts/title").asText(),sample.path("sourceUrl").asText());
        }
        var store = new JobAnalysisStore(db,new DataSourceTransactionManager(ds),json,ai,true);
        check(store.process(0,2)==2, "real Java client and transactional store completed two exports");
        for (int i=1;i<=2;i++) {
            var row=db.queryForMap("SELECT d.detail_data,d.job_requirements,d.employee_benefits,a.source_json,a.result_json FROM t_job_detail d JOIN job_ai_analysis a ON a.job_id=d.id WHERE d.id=?",i);
            JsonNode result=json.readTree((String)row.get("result_json"));
            check(json.readTree((String)row.get("source_json")).equals(samples.get(i-1).path("detailData")), "source snapshot equals exact collector export "+i);
            check(json.readTree((String)row.get("job_requirements")).equals(json.valueToTree(ai.texts(result,"requirements"))) && json.readTree((String)row.get("employee_benefits")).equals(json.valueToTree(ai.texts(result,"benefits"))), "transaction projected exact model text arrays "+i);
            check(result.path("requirements").toString().contains("Python, SQL (test) 😀") && result.path("benefits").toString().contains("五险一金、带薪年假。"), "Unicode and punctuation survive model validation and projection "+i);
            save("analysis-"+i+".json", result);
        }
        for(String state:List.of("failed","needs_review")) {
            JsonNode original=samples.get(0).path("detailData");
            var intervening=(com.fasterxml.jackson.databind.node.ObjectNode)original.deepCopy();
            intervening.putArray("jobTags").add("synthetic intervening "+state);
            db.update("UPDATE t_job_detail SET detail_data=? WHERE id=1",intervening.toString());
            var lease=store.claim(1,intervening.toString());
            check(lease!=null, "intervening source obtains lease: "+state);
            if(state.equals("failed"))store.fail(lease,"synthetic_terminal",false);
            else {
                var review=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(Files.readString(out.resolve("analysis-1.json")));
                review.put("status","needs_review");store.finish(lease,review);
            }
            db.update("UPDATE t_job_detail SET detail_data=? WHERE id=1",original.toString());
            long calls;try(var lines=Files.lines(out.resolve("provider-requests.jsonl"))){calls=lines.count();}
            check(store.process(0,1)==1, "completed source restored after intervening "+state);
            try(var lines=Files.lines(out.resolve("provider-requests.jsonl"))){check(lines.count()==calls, "restoration uses validated cache without provider call: "+state);}
            String readTable=JobAnalysisReader.table(true);
            check(db.queryForObject("SELECT analysis_state FROM "+readTable+" WHERE id=1",String.class).equals("completed"), "reader selects restored completion: "+state);
            check(store.process(0,1)==0, "restored completion is not claimed again: "+state);
        }
        Files.createFile(out.resolve("fail-provider"));
        try { check(store.process(2,3)==0, "upstream HTTP 503 yields zero completed writebacks"); }
        finally { Files.delete(out.resolve("fail-provider")); }
        check(db.queryForObject("SELECT job_requirements FROM t_job_detail WHERE id=3",String.class).equals("[]") && db.queryForObject("SELECT employee_benefits FROM t_job_detail WHERE id=3",String.class).equals("[]"), "upstream failure leaves both projection arrays untouched");
        save("failed-row.json", db.queryForMap("SELECT state,error_code,attempt_count FROM job_ai_analysis WHERE job_id=3"));
        var jobs = new ArrayList<com.fasterxml.jackson.databind.node.ObjectNode>();
        for (int i=1;i<=2;i++) jobs.add(json.createObjectNode().put("jobId",Integer.toString(i)).set("detailData",samples.get(i-1).path("detailData")));
        JsonNode match = ai.match("纯合成简历：熟练 Python, SQL (test) 😀；具有三年项目经验，本科学历。", jobs, 30000);
        save("match.json",match);
        for (JsonNode item:match.path("items")) {
            check(item.path("status").asText().equals("scored"), "each match item has actual scored status");
            for (JsonNode dimension:item.path("dimensions")) check(dimension.path("status").asText().equals("scored") && dimension.path("rawScore").isNumber(), "match dimension has numeric scored evidence");
        }
        check(match.path("complete").asBoolean(), "real match pipeline returns complete scored results");
        // Match item order is the production contract; ranked view is assembled explicitly below.
        check(Math.abs(match.path("items").get(0).path("score").asDouble()-8.75)<1e-9 && Math.abs(match.path("items").get(1).path("score").asDouble()-6.25)<1e-9, "actual weighted scored values 8.75 and 6.25");
        var config = new org.apache.ibatis.session.Configuration(new Environment("fixture",new JdbcTransactionFactory(),ds));
        config.setMapUnderscoreToCamelCase(true);
        try (var in=Workflow.class.getClassLoader().getResourceAsStream("mapper/ai/TJobDetailMapper.xml")) { new XMLMapperBuilder(in,config,"production",config.getSqlFragments()).parse(); }
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/fixture/v1/jobs",exchange -> {
            try (var session=new SqlSessionFactoryBuilder().build(config).openSession()) {
                var result=json.createObjectNode().put("schema",schema).put("synthetic",true);
                var items=result.putArray("items");
                String table;
                try (var connection=ds.getConnection()) { table=JobAnalysisReader.table(connection); }
                for (var mini:session.getMapper(TJobDetailMapper.class).selectMiniJobDetail(null,null,0L,10)) {
                    var row=db.queryForMap("SELECT * FROM "+table+" WHERE id=?",mini.getId());
                    var item=items.addObject();
                    item.set("job",json.valueToTree(new FormatDataService(json).convertToJobAnalysisData(mini)));
                    item.put("analysisStatus",JobAnalysisReader.status((String)row.get("detail_data"),(String)row.get("analysis_source"),(String)row.get("analysis_state"),(String)row.get("analysis_result"),(String)row.get("job_requirements"),(String)row.get("employee_benefits")));
                    item.set("sampling",samples.get(mini.getId().intValue()==2 ? 1 : 0));
                    item.put("model",(String)row.get("analysis_model")); item.put("pipelineVersion",(String)row.get("analysis_version"));
                }
                result.set("match",match);
                var ranked=new ArrayList<JsonNode>(); match.path("items").forEach(ranked::add);
                ranked.sort(Comparator.comparingDouble((JsonNode node)->node.path("score").asDouble()).reversed());
                result.set("rankedMatch",json.valueToTree(ranked));
                byte[] body=json.writeValueAsBytes(result);
                exchange.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");
                exchange.sendResponseHeaders(200,body.length); exchange.getResponseBody().write(body);
            } catch(Exception e) { exchange.sendResponseHeaders(500,-1); }
            finally { exchange.close(); }
        });
        server.start();
        try {
            String route="http://127.0.0.1:"+server.getAddress().getPort()+"/fixture/v1/jobs";
            Files.writeString(out.resolve("reader-route.txt"),route);
            var response=http.send(HttpRequest.newBuilder(URI.create(route)).build(),HttpResponse.BodyHandlers.ofString());
            check(response.statusCode()==200, "real MyBatis/FormatDataService/JobAnalysisReader DTO served over HTTP");
            Files.writeString(out.resolve("frontend-dto.json"), response.body());
            var dto=json.readTree(response.body());
            check(dto.at("/items/0/job/address").isNull(), "missing address remains JSON null over HTTP");
            check(dto.at("/items/0/job/jobUrl").asText().equals(dto.at("/items/0/sampling/sourceUrl").asText()), "stored source URL reaches HTTP job DTO");
            check(dto.at("/rankedMatch/0/jobId").asText().equals("1") && dto.at("/rankedMatch/1/jobId").asText().equals("2"), "HTTP ranked scored results descending");
            check(dto.at("/items/0/analysisStatus").asText().equals("complete") && dto.at("/items/2/analysisStatus").asText().equals("pending"), "HTTP reader exposes complete and failed retry-pending provenance states");
        } finally { server.stop(0); }
        save("result.json",Map.of("status","verified","schema",schema,"checks",checks,"synthetic",List.of("BOSS list/detail", "official provider inference", "resume"),"real",List.of("system Chromium", "RecruitmentSampler.collect", "PrivateOutput", "Python HTTP/FastAPI/Jev", "Java AI client", "JobAnalysisStore/MySQL transactions", "MyBatis/reader/DTO HTTP")));
    }
}

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import io.minio.messages.Item;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Isolated Java 17 / application MinIO SDK 8.5.7 verification and conservative copy. */
public class StorageCheck {
  record Store(MinioClient client, String bucket, String endpoint) {}
  static Store store(String file) throws Exception {
    Properties p = new Properties();
    try (var in = Files.newInputStream(Path.of(file))) { p.load(new InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)); }
    return new Store(MinioClient.builder().endpoint(p.getProperty("endpoint"))
        .credentials(p.getProperty("accessKey"), p.getProperty("secretKey")).build(), p.getProperty("bucket"), p.getProperty("endpoint"));
  }
  static void require(boolean ok, String message) { if (!ok) throw new IllegalStateException(message); }
  static byte[] digest(Store s, String key) throws Exception {
    var md = MessageDigest.getInstance("SHA-256");
    try (var in = s.client.getObject(GetObjectArgs.builder().bucket(s.bucket).object(key).build())) {
      byte[] buf = new byte[65536]; int n;
      while ((n = in.read(buf)) != -1) md.update(buf, 0, n);
    }
    return md.digest();
  }
  static StatObjectResponse stat(Store s, String key) throws Exception {
    return s.client.statObject(StatObjectArgs.builder().bucket(s.bucket).object(key).build());
  }
  static Map<String,String> tags(Store s, String key) throws Exception {
    return s.client.getObjectTags(GetObjectTagsArgs.builder().bucket(s.bucket).object(key).build()).get();
  }
  static List<String> keys(Store s) throws Exception {
    var keys = new ArrayList<String>();
    for (var result : s.client.listObjects(ListObjectsArgs.builder().bucket(s.bucket).recursive(true).build())) keys.add(result.get().objectName());
    return keys;
  }
  static boolean absent(ErrorResponseException e, String... codes) {
    return Arrays.asList(codes).contains(e.errorResponse().code());
  }
  // Fail closed: this copier supports only unversioned, unlocked, unencrypted private buckets.
  static List<String> inventory(Store s) throws Exception {
    var v = s.client.getBucketVersioning(GetBucketVersioningArgs.builder().bucket(s.bucket).build());
    require(v.status() == null || v.status() == io.minio.messages.VersioningConfiguration.Status.OFF, "BLOCK: versioning/history requires a separate migration");
    try {
      var lock = s.client.getObjectLockConfiguration(GetObjectLockConfigurationArgs.builder().bucket(s.bucket).build());
      require(lock == null, "BLOCK: Object Lock configuration present");
    } catch (ErrorResponseException e) { if (!absent(e,"ObjectLockConfigurationNotFoundError","NoSuchObjectLockConfiguration")) throw e; }
    try {
      var encryption = s.client.getBucketEncryption(GetBucketEncryptionArgs.builder().bucket(s.bucket).build());
      require(encryption == null || encryption.rule() == null, "BLOCK: bucket SSE");
    } catch (ErrorResponseException e) { if (!absent(e,"ServerSideEncryptionConfigurationNotFoundError")) throw e; }
    try {
      String policy = s.client.getBucketPolicy(GetBucketPolicyArgs.builder().bucket(s.bucket).build());
      require(policy == null || policy.isBlank(), "BLOCK: bucket policy needs separate review");
    } catch (ErrorResponseException e) { if (!absent(e,"NoSuchBucketPolicy")) throw e; }
    // Includes delete markers and non-current versions even when versioning is now suspended.
    for (var r : s.client.listObjects(ListObjectsArgs.builder().bucket(s.bucket).recursive(true).includeVersions(true).build())) {
      Item i = r.get();
      require(!i.isDeleteMarker() && (i.versionId() == null || i.versionId().equals("null")), "BLOCK: versions/delete markers");
    }
    List<String> keys = keys(s); long bytes = 0;
    for (String k : keys) {
      var st = stat(s,k); bytes += st.size(); tags(s,k);
      for (String h : st.headers().names()) {
        String name = h.toLowerCase(Locale.ROOT);
        require(!name.startsWith("x-amz-server-side-encryption") && !name.startsWith("x-amz-object-lock"), "BLOCK: object SSE/retention/legal hold");
      }
    }
    System.out.println("inventory objects="+keys.size()+" bytes="+bytes+"; versions/markers/locks/SSE/policy checked");
    return keys;
  }
  static Map<String,String> headers(StatObjectResponse st) {
    Map<String,String> h = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    for (String name : List.of("Cache-Control","Content-Disposition","Content-Encoding","Content-Language","Expires")) {
      if (st.headers().get(name) != null) h.put(name, st.headers().get(name));
    }
    return h;
  }
  static void verify(Store source, Store target, List<String> sourceKeys) throws Exception {
    require(new HashSet<>(sourceKeys).equals(new HashSet<>(keys(target))), "BLOCK: key sets differ (no automatic deletes)");
    for (String k : sourceKeys) {
      var a = stat(source,k); var b = stat(target,k);
      require(a.size()==b.size() && Objects.equals(a.contentType(),b.contentType()) && a.userMetadata().equals(b.userMetadata())
          && headers(a).equals(headers(b)) && tags(source,k).equals(tags(target,k)) && Arrays.equals(digest(source,k),digest(target,k)), "BLOCK: object bytes/metadata/tags mismatch");
    }
    System.out.println("verified exact keys, bytes (SHA-256), content type, user metadata, HTTP metadata, tags");
  }
  static void copy(Store source, Store target, List<String> list) throws Exception {
    require(keys(target).isEmpty(),"BLOCK: copy target must be empty; no overwrite");
    for(String k:list) {
      var st=stat(source,k);
      try(var in=source.client.getObject(GetObjectArgs.builder().bucket(source.bucket).object(k).build())) {
        target.client.putObject(PutObjectArgs.builder().bucket(target.bucket).object(k).contentType(st.contentType())
            .userMetadata(st.userMetadata()).headers(headers(st)).tags(tags(source,k)).stream(in,st.size(),-1).build());
      }
    }
  }
  static void probe(Store s) throws Exception {
    // Never use the application bucket for synthetic writes; a fresh dedicated bucket is mandatory.
    String bucket = "store-check-"+UUID.randomUUID(); s = new Store(s.client,bucket,s.endpoint);
    s.client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
    String key="1_中文 空格.png", big="1_multipart.bin";
    try {
      require(s.client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()),"bucket missing");
      byte[] payload="synthetic image bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8);
      s.client.putObject(PutObjectArgs.builder().bucket(bucket).object(key).contentType("image/png")
          .userMetadata(Map.of("fixture","synthetic")).tags(Map.of("purpose","compatibility"))
          .stream(new ByteArrayInputStream(payload),payload.length,-1).build());
      try (var in=s.client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) { require(Arrays.equals(payload,in.readAllBytes()),"GET mismatch"); }
      require(stat(s,key).contentType().equals("image/png"),"content type mismatch");
      String url=s.client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().bucket(bucket).object(key).method(Method.GET).expiry(7,java.util.concurrent.TimeUnit.DAYS).build());
      var http=HttpClient.newHttpClient();
      var response=http.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
      require(response.statusCode()==200 && Arrays.equals(payload,response.body()),"presigned GET failed");
      String tampered=url.replace("X-Amz-Expires=604800","X-Amz-Expires=604799");
      require(!tampered.equals(url),"tamper setup failed");
      require(http.send(HttpRequest.newBuilder(URI.create(tampered)).GET().build(),HttpResponse.BodyHandlers.discarding()).statusCode()==403,"tampered signature accepted");
      require(http.send(HttpRequest.newBuilder(URI.create(url.substring(0,url.indexOf('?')))).GET().build(),HttpResponse.BodyHandlers.discarding()).statusCode()==403,"anonymous read accepted");
      byte[] large=new byte[11*1024*1024];new Random(37).nextBytes(large);
      s.client.putObject(PutObjectArgs.builder().bucket(bucket).object(big).contentType("application/octet-stream")
          .stream(new ByteArrayInputStream(large),large.length,5*1024*1024).build());
      require(Arrays.equals(MessageDigest.getInstance("SHA-256").digest(large),digest(s,big)),"multipart bytes mismatch");
      inventory(s);
      System.out.println("PASS bucket/put/get/delete path, Unicode+space, content type, metadata/tags, 7-day presign, tamper 403, anonymous 403, 11MiB multipart");
    } finally {
      for (String k : keys(s)) s.client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(k).build());
      require(keys(s).isEmpty(),"delete failed");
      s.client.removeBucket(RemoveBucketArgs.builder().bucket(bucket).build());
    }
  }
  public static void main(String[] args) {
    try {
      require(args.length>=2,"Usage: probe|audit FILE; copy|verify SOURCE TARGET [REVIEW]");
      Store source=store(args[1]);
      switch(args[0]) {
        case "probe" -> probe(source);
        case "audit" -> inventory(source);
        case "copy", "verify" -> {
          Store target=store(args[2]);
          require(!((source.endpoint.replaceAll("/+$","")+"/"+source.bucket).equals(target.endpoint.replaceAll("/+$","")+"/"+target.bucket)),"same source and target");
          List<String> list=inventory(source); inventory(target);
          if(args[0].equals("copy")) {
            Properties review=new Properties();try(var in=Files.newInputStream(Path.of(args[3]))) {review.load(in);}
            for(String item:List.of("backupRestoredAndVerified","writersFrozen","iamAclLifecycleReplicationReviewed","specialFeaturesAbsent")) require("true".equals(review.getProperty(item)),"BLOCK: operator review missing: "+item);
            require(Files.isRegularFile(Path.of(review.getProperty("backupEvidenceFile",""))),"BLOCK: backup evidence missing");
            copy(source,target,list);
          }
          verify(source,target,list);
          require(new HashSet<>(list).equals(new HashSet<>(inventory(source))),"BLOCK: source changed during run");
          System.out.println("No cutover performed; frozen writers and reviewed IAM remain operator responsibilities");
        }
        default -> throw new IllegalArgumentException("unknown mode");
      }
      System.exit(0); // All synchronous operations completed; close SDK executor lifetime with this CLI.
    } catch(Exception e) {
      // Do not print signed URLs, credentials or SDK request/response dumps.
      System.err.println("FAILED "+e.getClass().getSimpleName()+(e instanceof ErrorResponseException r?" S3="+r.errorResponse().code():e instanceof IllegalStateException?" "+e.getMessage():"; inspect private configuration/connectivity"));
      System.exit(1);
    }
  }
}

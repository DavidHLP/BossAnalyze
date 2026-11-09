import io.minio.*;
import io.minio.messages.VersioningConfiguration;
import java.io.*;
import java.util.*;

/** Real target only; creates/removes dedicated random buckets, never application data. */
public class StorageCheckTest {
  static void blocked(Checked task) throws Exception {
    try { task.run(); } catch (IllegalStateException expected) { return; }
    throw new AssertionError("unsafe operation was accepted");
  }
  interface Checked { void run() throws Exception; }
  public static void main(String[] args) {
    try { run(args); System.exit(0); }
    catch (Exception e) { System.err.println("FAILED " + e.getClass().getSimpleName()); System.exit(1); }
  }
  static void run(String[] args) throws Exception {
    var base=StorageCheck.store(args[0]);
    var source=new StorageCheck.Store(base.client(),"copy-source-"+UUID.randomUUID(),base.endpoint());
    var target=new StorageCheck.Store(base.client(),"copy-target-"+UUID.randomUUID(),base.endpoint());
    for(var s:List.of(source,target)) s.client().makeBucket(MakeBucketArgs.builder().bucket(s.bucket()).build());
    String key="旧 图片.png";
    try {
      byte[] bytes="synthetic migration fixture".getBytes(java.nio.charset.StandardCharsets.UTF_8);
      source.client().putObject(PutObjectArgs.builder().bucket(source.bucket()).object(key).contentType("image/png")
          .headers(Map.of("Cache-Control","private, max-age=60","Content-Disposition","inline"))
          .userMetadata(Map.of("migration","fixture")).tags(Map.of("fixture","true"))
          .stream(new ByteArrayInputStream(bytes),bytes.length,-1).build());
      var keys=StorageCheck.inventory(source);StorageCheck.inventory(target);
      StorageCheck.copy(source,target,keys);StorageCheck.verify(source,target,keys);
      blocked(()->StorageCheck.copy(source,target,keys));
      target.client().putObject(PutObjectArgs.builder().bucket(target.bucket()).object(key).contentType("image/png")
          .stream(new ByteArrayInputStream(new byte[]{1}),1,-1).build());
      blocked(()->StorageCheck.verify(source,target,keys));
      target.client().removeObject(RemoveObjectArgs.builder().bucket(target.bucket()).object(key).build());
      blocked(()->StorageCheck.verify(source,target,keys));
      source.client().removeObject(RemoveObjectArgs.builder().bucket(source.bucket()).object(key).build());
      source.client().setBucketVersioning(SetBucketVersioningArgs.builder().bucket(source.bucket())
          .config(new VersioningConfiguration(VersioningConfiguration.Status.ENABLED,null)).build());
      blocked(()->StorageCheck.inventory(source));
      System.out.println("PASS real synthetic S3 copy/reconcile; rejects nonempty target, changed object, missing key, versioned source");
    } finally {
      for(var s:List.of(source,target)) {
        for(String k:StorageCheck.keys(s)) s.client().removeObject(RemoveObjectArgs.builder().bucket(s.bucket()).object(k).build());
        s.client().removeBucket(RemoveBucketArgs.builder().bucket(s.bucket()).build());
      }
    }
  }
}

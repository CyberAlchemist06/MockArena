package com.mockarena.runner;

import org.junit.jupiter.api.Test; import org.junit.jupiter.api.condition.EnabledIfSystemProperty; import java.io.*; import java.nio.charset.StandardCharsets; import java.util.*; import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

/** Guarded local diagnostic: exposes bounded compiler stderr only in this assertion. */
@EnabledIfSystemProperty(named="mockarena.runner.docker-debug",matches="true") class DockerJavaSandboxDebugTest {
 @Test void fixedSourceCompilesUnderTheExactIsolationProfile() throws Exception {
  List<String> command=List.of("docker","run","--rm","-i","--network","none","--cap-drop","ALL","--security-opt","no-new-privileges","--read-only","--tmpfs","/workspace:rw,exec,nosuid,nodev,mode=1777,size=64m","--tmpfs","/tmp:rw,exec,nosuid,nodev,mode=1777,size=64m","--tmpfs","/home/mockarena:rw,exec,nosuid,nodev,mode=1777,size=32m","--memory","256m","--cpus","1.0","--pids-limit","64","--user","10001:10001","--workdir","/workspace","--entrypoint","sh","mockarena-java-runner:21-v1","-c","cat > Main.java && test -r Main.java && javac Main.java && test -f Main.class && id -u && pwd");
  Process p=new ProcessBuilder(command).start();try(OutputStream os=p.getOutputStream()){os.write("public class Main { public static void main(String[] args) { System.out.println(\"OK\"); } }".getBytes(StandardCharsets.UTF_8));}boolean done=p.waitFor(15,TimeUnit.SECONDS);String stdout=new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8),stderr=new String(p.getErrorStream().readAllBytes(),StandardCharsets.UTF_8);String diagnostic=(stderr+" | stdout="+stdout);assertThat(done).as("bounded diagnostic: %s",diagnostic.substring(0,Math.min(1024,diagnostic.length()))).isTrue();assertThat(p.exitValue()).as("bounded javac diagnostic: %s",diagnostic.substring(0,Math.min(1024,diagnostic.length()))).isZero();assertThat(stdout).contains("10001").contains("/workspace");
 }
}

package com.mockarena.runner.infrastructure;

import com.mockarena.runner.api.*;
import com.mockarena.runner.application.NormalizedWhitespaceComparator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.*; import java.nio.charset.StandardCharsets; import java.nio.file.*; import java.time.*; import java.util.*; import java.util.concurrent.*;

/** Docker is controlled by this private service only; candidate containers receive neither its socket nor credentials. */
@Component public class DockerJavaSandbox {
  private final String docker, image; private final boolean enabled; private final int maxMemory, maxOutput, maxProcesses; private final double maxCpus;
  public DockerJavaSandbox(@Value("${runner.docker.executable:docker}") String docker,@Value("${runner.docker.image}") String image,@Value("${runner.docker.enabled:true}") boolean enabled,@Value("${runner.docker.max-memory-mb:512}") int maxMemory,@Value("${runner.docker.max-output-bytes:1048576}") int maxOutput,@Value("${runner.docker.max-processes:32}") int maxProcesses,@Value("${runner.docker.max-cpus:1.0}") double maxCpus){this.docker=docker;this.image=image;this.enabled=enabled;this.maxMemory=maxMemory;this.maxOutput=maxOutput;this.maxProcesses=maxProcesses;this.maxCpus=maxCpus;}
  public ExecutionResponse execute(ExecutionRequest r) {
    if(!enabled) return infrastructure(r,"RUNNER_DISABLED");
    String container=null;
    try {
      int memory=Math.min(r.limits().memoryMb(),maxMemory), processes=Math.min(r.limits().maxProcesses(),maxProcesses), output=Math.min(r.limits().maxOutputBytes(),maxOutput);
      container=command(List.of("create","--label","mockarena.runner.job="+r.jobId(),"--network","none","--cap-drop","ALL","--security-opt","no-new-privileges","--read-only","--tmpfs","/workspace:rw,exec,nosuid,nodev,mode=1777,size=64m","--tmpfs","/tmp:rw,exec,nosuid,nodev,mode=1777,size=64m","--tmpfs","/home/mockarena:rw,exec,nosuid,nodev,mode=1777,size=32m","--memory",memory+"m","--cpus",Double.toString(maxCpus),"--pids-limit",Integer.toString(processes),"--user","10001:10001","--workdir","/workspace","--entrypoint","sleep",image,"infinity"),null,Duration.ofSeconds(20),1024).stdout().trim();
      command(List.of("start",container),null,Duration.ofSeconds(20),1024);
      CommandResult copy=command(List.of("exec","-i",container,"sh","-c","cat > Main.java"),r.source(),Duration.ofSeconds(20),1024);
      if(copy.timedOut()||copy.exit()!=0) return infrastructure(r,"SOURCE_TRANSFER_FAILED");
      CommandResult compilation=command(List.of("exec",container,"javac","Main.java"),null,Duration.ofMillis(r.limits().compileTimeoutMs()),output);
      if(compilation.timedOut()) return candidate(r,"TIME_LIMIT_EXCEEDED",0,0,null);
      if(compilation.exit()!=0) return candidate(r,"COMPILE_ERROR",0,0,null);
      int passed=0; long elapsed=0;
      for(ExecutionRequest.TestCase test:r.tests()) { CommandResult run=command(List.of("exec","-i",container,"java","Main"),test.input(),Duration.ofMillis(r.limits().executionTimeoutMs()),output); elapsed+=run.elapsedMs(); if(run.outputExceeded())return candidate(r,"OUTPUT_LIMIT_EXCEEDED",passed,r.tests().size(),elapsed); if(run.timedOut())return candidate(r,"TIME_LIMIT_EXCEEDED",passed,r.tests().size(),elapsed); if(run.exit()!=0)return candidate(r,"RUNTIME_ERROR",passed,r.tests().size(),elapsed); if(NormalizedWhitespaceComparator.matches(run.stdout(),test.expectedOutput()))passed++; }
      return new ExecutionResponse(r.jobId(),"COMPLETED",null,passed,r.tests().size(),elapsed,null);
    } catch(Exception e) { return infrastructure(r,"DOCKER_EXECUTION_FAILED"); }
    finally { if(container!=null) try{command(List.of("rm","-f",container),null,Duration.ofSeconds(20),1024);}catch(Exception ignored){} }
  }
  private ExecutionResponse candidate(ExecutionRequest r,String category,int passed,int total,Long ms){return new ExecutionResponse(r.jobId(),"COMPLETED",category,passed,total,ms,null);} private ExecutionResponse infrastructure(ExecutionRequest r,String c){return new ExecutionResponse(r.jobId(),"INFRASTRUCTURE_FAILURE",c,0,r.tests().size(),null,null);}
  private CommandResult command(List<String> args,String stdin,Duration timeout,int limit)throws Exception { List<String> full=new ArrayList<>();full.add(docker);full.addAll(args); Process p=new ProcessBuilder(full).start(); if(stdin!=null){try(OutputStream os=p.getOutputStream()){os.write(stdin.getBytes(StandardCharsets.UTF_8));}} long start=System.nanoTime(); Future<String> out=read(p.getInputStream(),limit), err=read(p.getErrorStream(),limit); boolean done=p.waitFor(timeout.toMillis(),TimeUnit.MILLISECONDS);if(!done)p.destroyForcibly(); if(!done)p.waitFor(5,TimeUnit.SECONDS);String stdout=out.get(5,TimeUnit.SECONDS), stderr=err.get(5,TimeUnit.SECONDS);return new CommandResult(done?p.exitValue():-1,!done,stdout,stderr,stdout.length()+stderr.length()>limit,(System.nanoTime()-start)/1_000_000); }
  private Future<String> read(InputStream in,int limit){return Executors.newVirtualThreadPerTaskExecutor().submit(()->{try(in){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] x=new byte[4096];int n;while((n=in.read(x))!=-1){if(b.size()+n>limit){b.write(x,0,Math.max(0,limit-b.size()));break;}b.write(x,0,n);}return b.toString(StandardCharsets.UTF_8);} });}
  private record CommandResult(int exit,boolean timedOut,String stdout,String stderr,boolean outputExceeded,long elapsedMs){}
}

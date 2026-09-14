package com.mockarena.assessment.api;

import com.mockarena.assessment.domain.*;
import com.mockarena.assessment.application.AttemptNotFoundException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant; import java.util.*;

/** Workload-only source retrieval; never mounted through the BFF. */
@RestController @RequestMapping("/internal/v1/coding-snapshots")
public class InternalCodingSnapshotController {
 private final SubmittedCodingResponseSnapshotRepository snapshots;
 public InternalCodingSnapshotController(SubmittedCodingResponseSnapshotRepository snapshots){this.snapshots=snapshots;}
 @GetMapping("/attempts/{attemptId}/items/{globalPosition}") public Snapshot get(@PathVariable UUID attemptId,@PathVariable int globalPosition){SubmittedCodingResponseSnapshot s=snapshots.findById(new SubmittedCodingResponseSnapshotId(attemptId,globalPosition)).orElseThrow(AttemptNotFoundException::new);return new Snapshot(s.attemptId(),s.globalPosition(),s.questionId(),s.questionVersionId(),s.programmingLanguage(),s.sourceCode(),s.responseVersion(),s.sourceFingerprint(),"UNANSWERED".equals(s.responseState()),s.submittedAt());}
 public record Snapshot(UUID attemptId,int globalPosition,UUID questionId,UUID questionVersionId,String programmingLanguage,String sourceCode,Long responseVersion,String sourceFingerprint,boolean unanswered,Instant submittedAt){}
}

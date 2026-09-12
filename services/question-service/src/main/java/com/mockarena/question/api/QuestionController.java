package com.mockarena.question.api;

import com.mockarena.question.application.QuestionApplicationService;
import com.mockarena.question.api.QuestionDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.*;
import com.mockarena.question.security.CurrentAuthenticatedUser;

@RestController @RequestMapping("/api/v1/questions")
public class QuestionController {
    private final QuestionApplicationService service; private final CurrentAuthenticatedUser currentUser;
    public QuestionController(QuestionApplicationService service, CurrentAuthenticatedUser currentUser) { this.service=service; this.currentUser=currentUser; }
    @PostMapping public ResponseEntity<QuestionVersionResponse> create(@Valid @RequestBody CreateQuestionRequest request) {
        QuestionVersionResponse response=QuestionDtos.toResponse(service.create(request,currentUser.userId()));
        return ResponseEntity.created(URI.create("/api/v1/questions/"+response.questionId()+"/versions/"+response.versionNumber())).body(response);
    }
    @GetMapping("/{questionId}") public QuestionResponse question(@PathVariable UUID questionId) { return QuestionDtos.toResponse(service.getQuestion(questionId)); }
    @GetMapping("/{questionId}/versions") public List<QuestionVersionResponse> versions(@PathVariable UUID questionId) { return service.listVersions(questionId).stream().map(QuestionDtos::toResponse).toList(); }
    @GetMapping("/{questionId}/versions/{number}") public QuestionVersionResponse version(@PathVariable UUID questionId,@PathVariable int number) { return QuestionDtos.toResponse(service.getVersion(questionId,number)); }
    @PutMapping("/{questionId}/versions/{number}") public QuestionVersionResponse update(@PathVariable UUID questionId,@PathVariable int number,@Valid @RequestBody UpdateVersionRequest request) { return QuestionDtos.toResponse(service.update(questionId,number,request)); }
    @PostMapping("/{questionId}/versions") @ResponseStatus(HttpStatus.CREATED) public QuestionVersionResponse revise(@PathVariable UUID questionId,@Valid @RequestBody CreateRevisionRequest request) { return QuestionDtos.toResponse(service.createRevision(questionId,request)); }
    @PostMapping("/{questionId}/versions/{number}/publish") public QuestionVersionResponse publish(@PathVariable UUID questionId,@PathVariable int number,@Valid @RequestBody PublishVersionRequest request) { return QuestionDtos.toResponse(service.publish(questionId,number,request)); }
}

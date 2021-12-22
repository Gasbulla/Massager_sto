package com.javamentor.qa.platform.webapp.controllers.rest;

import com.javamentor.qa.platform.models.entity.question.answer.VoteType;
import com.javamentor.qa.platform.models.entity.user.User;
import com.javamentor.qa.platform.service.abstracts.model.QuestionService;
import com.javamentor.qa.platform.service.abstracts.model.ReputationService;
import com.javamentor.qa.platform.service.abstracts.model.VoteQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Question Resource Controller", description = "Управление сущностями, которые связаны с вопросами")
public class QuestionResourceController {

    final
    QuestionService questionService;

    final
    VoteQuestionService voteQuestionService;

    final
    ReputationService reputationService;

    public QuestionResourceController(QuestionService questionService, VoteQuestionService voteQuestionService, ReputationService reputationService) {
        this.questionService = questionService;
        this.voteQuestionService = voteQuestionService;
        this.reputationService = reputationService;
    }

    @PostMapping("api/user/question/{id}/upVote")
    @Operation(
            summary = "Голосование ЗА вопрос",
            description = "Устанавливает голос +1 за вопрос и +10 к репутации автора вопроса"
    )
    public ResponseEntity<?> upVote(@PathVariable("id") Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user =(User)auth.getPrincipal();
        Long userId = user.getId();
        int countUpVote = 10;
        if (questionService.isQuestionValidate(id)) return new ResponseEntity<>("Can't find question with id:"+ id , HttpStatus.NOT_FOUND);
        if (voteQuestionService.validateUserVote(id, userId)) {
            voteQuestionService.setVoteAndSetReputation(userId, id, VoteType.UP_VOTE,countUpVote);
        }
        return new ResponseEntity<>(voteQuestionService.getVote(id), HttpStatus.OK);
    }

    @PostMapping("api/user/question/{id}/downVote")
    @Operation(
            summary = "Голосование ПРОТИВ вопроса",
            description = "Устанавливает голос -1 за вопрос и -5 к репутации автора вопроса"
    )
    public ResponseEntity<?> downVote(@PathVariable("id") Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user =(User)auth.getPrincipal();
        Long userId = user.getId();
        int countDownVote = -5;
        if (questionService.isQuestionValidate(id)) return new ResponseEntity<>("Can't find question with id:"+ id , HttpStatus.NOT_FOUND);
        if (voteQuestionService.validateUserVote(id, userId)) {
            voteQuestionService.setVoteAndSetReputation(userId, id, VoteType.DOWN_VOTE,countDownVote);
        }
        return new ResponseEntity<>(voteQuestionService.getVote(id), HttpStatus.OK);
    }
}


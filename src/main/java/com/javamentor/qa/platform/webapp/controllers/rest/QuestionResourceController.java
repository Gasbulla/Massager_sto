package com.javamentor.qa.platform.webapp.controllers.rest;

import com.javamentor.qa.platform.dao.impl.pagination.*;
import com.javamentor.qa.platform.exception.ConstrainException;
import com.javamentor.qa.platform.models.dto.PageDTO;
import com.javamentor.qa.platform.models.dto.QuestionCreateDto;
import com.javamentor.qa.platform.models.dto.QuestionDto;
import com.javamentor.qa.platform.models.dto.QuestionViewDto;
import com.javamentor.qa.platform.models.dto.question.QuestionCommentDto;
import com.javamentor.qa.platform.models.entity.pagination.PaginationData;
import com.javamentor.qa.platform.models.entity.question.CommentQuestion;
import com.javamentor.qa.platform.models.entity.question.DateFilter;
import com.javamentor.qa.platform.models.entity.question.Question;
import com.javamentor.qa.platform.models.entity.question.VoteQuestion;
import com.javamentor.qa.platform.models.entity.question.answer.VoteType;
import com.javamentor.qa.platform.models.entity.user.User;
import com.javamentor.qa.platform.service.abstracts.dto.QuestionDtoService;
import com.javamentor.qa.platform.service.abstracts.dto.TagDtoService;
import com.javamentor.qa.platform.service.abstracts.model.*;
import com.javamentor.qa.platform.webapp.converters.QuestionConverter;
import com.javamentor.qa.platform.webapp.converters.TagConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@Tag(name = "Question Resource Controller", description = "???????????????????? ????????????????????, ?????????????? ?????????????? ?? ??????????????????")
public class QuestionResourceController {

    private final QuestionService questionService;
    private final VoteQuestionService voteQuestionService;
    private final ReputationService reputationService;
    private final QuestionDtoService questionDtoService;
    private final QuestionConverter questionConverter;
    private final TagConverter tagConverter;
    private final TagDtoService tagDtoService;
    private final QuestionViewedService questionViewedService;
    private final BookmarksService bookmarksService;
    private final CommentQuestionService commentQuestionService;

    public QuestionResourceController(QuestionService questionService,
                                      VoteQuestionService voteQuestionService,
                                      ReputationService reputationService,
                                      QuestionDtoService questionDtoService,
                                      QuestionConverter questionConverter,
                                      TagConverter tagConverter,
                                      TagDtoService tagDtoService,
                                      QuestionViewedService questionViewedService,
                                      BookmarksService bookmarksService,
                                      CommentQuestionService commentQuestionService
    ) {
        this.questionService = questionService;
        this.voteQuestionService = voteQuestionService;
        this.reputationService = reputationService;
        this.questionDtoService = questionDtoService;
        this.questionConverter = questionConverter;
        this.tagConverter = tagConverter;
        this.tagDtoService = tagDtoService;
        this.questionViewedService = questionViewedService;
        this.bookmarksService = bookmarksService;
        this.commentQuestionService = commentQuestionService;
    }

    @GetMapping("api/user/question/count")
    @Operation(summary = "???????????????????? ?????????? ???????????????? ?? ????")
    @ApiResponse(responseCode = "200", description = "OK", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = Question.class))
    })
    @ApiResponse(responseCode = "400", description = "???????????????? ?????????????? ????????????", content = {
            @Content(mediaType = "application/json")
    })
    public ResponseEntity<Optional<Long>> getCountQuestion() {
        Optional<Long> countQuestion = questionService.getCountByQuestion();
        return new ResponseEntity<>(countQuestion, HttpStatus.OK);
    }

    @GetMapping("api/user/question/{questionId}/comment")
    @Operation(summary = "???????????????? ???????????? ?????????????????????????? ?? ??????????????")
    @ApiResponse(responseCode = "200", description = "OK", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = CommentQuestion.class))
    })
    @ApiResponse(responseCode = "400", description = "???????????????? ?????????????? ????????????", content = {
            @Content(mediaType = "application/json")
    })
    public ResponseEntity<List<QuestionCommentDto>> getQuestionIdComment(@PathVariable("questionId") Long questionId) {
        List<QuestionCommentDto> questionIdComment = questionDtoService.getQuestionByIdComment(questionId);
        return new ResponseEntity<>(questionIdComment, HttpStatus.OK);
    }

    @PostMapping("api/user/question/{questionId}/upVote")
    @Operation(
            summary = "?????????????????????? ???? ????????????",
            description = "?????????????????????????? ?????????? +1 ???? ???????????? ?? +10 ?? ?????????????????? ???????????? ??????????????"
    )
    public ResponseEntity<?> upVote(@PathVariable("questionId") Long questionId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();
        Long userId = user.getId();
        Question question = questionService
                .getQuestionByIdWithAuthor(questionId)
                .orElseThrow(() -> new ConstrainException("Can't find question with id:" + questionId));
        int countUpVote = 10;
        if (voteQuestionService.validateUserVoteByQuestionIdAndUserId(questionId, userId)) {
            VoteQuestion voteQuestion = new VoteQuestion(user, question, VoteType.UP_VOTE, countUpVote);
            voteQuestionService.persist(voteQuestion);
            return new ResponseEntity<>(voteQuestionService.getVoteByQuestionId(questionId), HttpStatus.OK);
        }
        return new ResponseEntity<>("User was voting", HttpStatus.BAD_REQUEST);
    }

    @PostMapping("api/user/question/{questionId}/downVote")
    @Operation(
            summary = "?????????????????????? ???????????? ??????????????",
            description = "?????????????????????????? ?????????? -1 ???? ???????????? ?? -5 ?? ?????????????????? ???????????? ??????????????"
    )
    public ResponseEntity<?> downVote(@PathVariable("questionId") Long questionId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();
        Long userId = user.getId();
        Question question = questionService
                .getQuestionByIdWithAuthor(questionId)
                .orElseThrow(() -> new ConstrainException("Can't find question with id:" + questionId));
        int countDownVote = -5;
        if (voteQuestionService.validateUserVoteByQuestionIdAndUserId(questionId, userId)) {
            VoteQuestion voteQuestion = new VoteQuestion(user, question, VoteType.DOWN_VOTE, countDownVote);
            voteQuestionService.persist(voteQuestion);
            return new ResponseEntity<>(voteQuestionService.getVoteByQuestionId(questionId), HttpStatus.OK);
        }
        return new ResponseEntity<>("User was voting", HttpStatus.BAD_REQUEST);
    }

    @GetMapping("api/user/question/{id}")
    @Operation(summary = "?????????????????? ???????????????????? ???? ?????????????? ????????????????????????")
    @ApiResponse(responseCode = "200", description = "???????????????????? ???? ??????????????", content = {
            @Content(mediaType = "application/json")
    })

    public ResponseEntity<?> getQuestion(@PathVariable Long id) {
        Optional<QuestionDto> q = questionDtoService.getQuestionDtoServiceById(id);
        if (q.isPresent()) {
            return new ResponseEntity<>(q.get(), HttpStatus.OK);
        }
        return new ResponseEntity<>("Question number not exist!", HttpStatus.BAD_REQUEST);
    }

    @Operation(
            summary = "???????????????????? ??????????????",
            description = "???????????????????? ??????????????"
    )
    @ApiResponse(responseCode = "200", description = "???????????? ????????????????", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = QuestionCreateDto.class))
    })
    @ApiResponse(responseCode = "400", description = "???????????? ???? ????????????????", content = {
            @Content(mediaType = "application/json")
    })
    @PostMapping("api/user/question")
    public ResponseEntity<?> createNewQuestion(@Valid @RequestBody QuestionCreateDto questionCreateDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Question question = questionConverter.questionDtoToQuestion(questionCreateDto);
        question.setUser((User) authentication.getPrincipal());
        question.setTags(tagConverter.listTagDtoToListTag(questionCreateDto.getTags()));
        questionService.persist(question);
        return new ResponseEntity<>(questionConverter.questionToQuestionDto(question), HttpStatus.OK);
    }


    @GetMapping("api/user/question/tag/{id}")
    @Operation(
            summary = "?????????????????? ???????????? ???????????????? ???? tag id",
            description = "?????????????????? ?????????????????????????????? ???????????? dto ???????????????? ???? id ????????"
    )
    @ApiResponse(
            responseCode = "200",
            description = "???????????????????? ???????????????????????????? ???????????? QuestionDto " +
                    "(id, title, authorId, authorReputation, authorName, authorImage, description, viewCount," +
                    "countAnswer, countValuable, persistDateTime, lastUpdateDateTime, listTagDto)",
            content = {
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = QuestionViewDto.class)
                    )
            }
    )
      public ResponseEntity<PageDTO<QuestionViewDto>> getPageQuestionsByTagId(@PathVariable Long id,
                                                                            @RequestParam int page,
                                                                            @RequestParam(defaultValue = "10") int items,
                                                                            @RequestParam(required = false, defaultValue = "ALL") DateFilter dateFilter) {
        PaginationData data = new PaginationData(
                page, items, QuestionPageDtoDaoByTagId.class.getSimpleName()
        );
        data.getProps().put("id", id);
        data.getProps().put("dateFilter", dateFilter.getDay());

        return new ResponseEntity<>(questionDtoService.getPageDto(data), HttpStatus.OK);
    }

    @GetMapping("api/user/question/new")
    @Operation(
            summary = "?????????????????? ????????????????",
            description = "???????????????????? ???? ???????? ????????????????????(?????????????? ?????????? ??????????)"
    )
    public ResponseEntity<PageDTO<QuestionViewDto>> getQuestionsSortedByDate(@RequestParam int page,
                                                                             @RequestParam(defaultValue = "10") int items,
                                                                             @RequestParam(required = false) List<Long> trackedTag,
                                                                             @RequestParam(required = false) List<Long> ignoredTag,
                                                                             @RequestParam(required = false, defaultValue = "ALL") DateFilter dateFilter) {
        PaginationData data = new PaginationData(page, items,
                QuestionPageDtoDaoSortedByDate.class.getSimpleName());
        data.getProps().put("trackedTag", trackedTag);
        data.getProps().put("ignoredTag", ignoredTag);
        data.getProps().put("dateFilter", dateFilter.getDay());


        return new ResponseEntity<>(questionDtoService.getPageDto(data), HttpStatus.OK);
    }

    @GetMapping("api/user/question/noAnswer")
    @Operation(summary = "?????????????????? ?????????????????????????????? ???????????? ???????? ????????????????, ???? ?????????????? ?????? ???? ?????? ??????????. " +
            "?? ?????????????? ?????????????????? page - ?????????? ????????????????, items (???? ?????????????????? 10) - ???????????????????? ?????????????????????? ???? ????????????????",
            description = "?????????????????? ?????????????????????????????? ???????????? ???????? ????????????????, ???? ?????????????? ?????? ???? ?????? ??????????.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "???????????????????? ???????????????????????????? ???????????? PageDTO<QuestionDTO> (id, title, authorId" +
                            "authorReputation, authorName, authorImage, description, viewCount, countAnswer" +
                            "countValuable, LocalDateTime, LocalDateTime, listTagDto",
                    content = {
                            @Content(
                                    mediaType = "application/json")
                    }),
    })
    public ResponseEntity<PageDTO<QuestionViewDto>> getQuestionsWithNoAnswer(@RequestParam int page,
                                                                             @RequestParam(required = false, defaultValue = "10") int items,
                                                                             @RequestParam(required = false) List<Long> trackedTag,
                                                                             @RequestParam(required = false) List<Long> ignoredTag,
                                                                             @RequestParam(required = false, defaultValue = "ALL") DateFilter dateFilter) {

        PaginationData data = new PaginationData(page, items, QuestionPageDtoDaoByNoAnswersImpl.class.getSimpleName());
        data.getProps().put("trackedTags", trackedTag);
        data.getProps().put("ignoredTags", ignoredTag);
        data.getProps().put("dateFilter", dateFilter.getDay());
        data.getProps().put("userId", ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId());

        return new ResponseEntity<>(questionDtoService.getPageDto(data), HttpStatus.OK);
    }

    @GetMapping("/api/user/question")
    @Operation(summary = "?????????????????? ?????????????????????????????? ???????????? ???????????????? ?? ???????????????????????? ?????????? trackedTag ?? ignoredTag",
            description = "?????????????????? ?????????????????????????????? ???????????? ???????????????? ????????????????????????, " +
                    "?? ?????????????? ?????????????????? page - ?????????? ????????????????, ???????????????????????? ????????????????, items (???? ?????????????????? 10) - ???????????????????? ?????????????????????? ???? ????????????????," +
                    "???? ???????????????????????? ???? ????????????, trackedTag - ???? ???????????????????????? ????????????????, ignoredTag - ???? ???????????????????????? ????????????????")
    @ApiResponse(responseCode = "200", description = "???????????????????? ???????????????????????????? ???????????? PageDTO<QuestionDTO> (id, title, authorId," +
            " authorReputation, authorName, authorImage, description, viewCount, countAnswer, countValuable," +
            " LocalDateTime, LocalDateTime, listTagDto", content = {
            @Content(mediaType = "application/json")
    })
    public ResponseEntity<PageDTO<QuestionViewDto>> allQuestionsWithTrackedTagsAndIgnoredTags(@RequestParam int page,
                                                                                              @RequestParam(required = false, defaultValue = "10") int items,
                                                                                              @RequestParam(required = false) List<Long> trackedTag,
                                                                                              @RequestParam(required = false) List<Long> ignoredTag,
                                                                                              @RequestParam(required = false, defaultValue = "ALL") DateFilter dateFilter,
                                                                                              Authentication auth) {

        PaginationData data = new PaginationData(page, items, QuestionPageDtoDaoAllQuestionsImpl.class.getSimpleName());
        User user = (User) auth.getPrincipal();
        data.getProps().put("trackedTags", trackedTag);
        data.getProps().put("ignoredTags", ignoredTag);
        data.getProps().put("userId", user.getId());
        data.getProps().put("dateFilter", dateFilter.getDay());

        return new ResponseEntity<>(questionDtoService.getPageDto(data), HttpStatus.OK);
    }

    @Operation(
            summary = "???????????????? ???????????? ?????? ??????????????????????",
            description = "???????????????? ???????????? ?????? ??????????????????????"
    )
    @ApiResponse(responseCode = "200", description = "?????????? ???????????????? ?????? ????????????", content = {
            @Content(mediaType = "application/json")
    })
    @ApiResponse(responseCode = "403", description = "???????????????????????? ???? ????????????????????????????????", content = {
            @Content(mediaType = "application/json")
    })
    @GetMapping("api/user/question/{id}/view")
    public ResponseEntity<String> markQuestionLikeViewed(@PathVariable Long id, Authentication auth) {

        User user = (User) auth.getPrincipal();
        Optional<Question> question = questionService.getById(id);

        if (question.isPresent()) {
            questionViewedService.markQuestionLikeViewed(user, question.get());
            return new ResponseEntity<>("OK", HttpStatus.OK);
        }

        return new ResponseEntity<>("There is no question " + id.toString(), HttpStatus.BAD_REQUEST);
    }

    @GetMapping("/api/user/popular")
    @Operation(summary = "?????????????????? ?????????????? ?????????????????????????????? ???????????? ???????????????????? ????????????????",
            description = "?????????????????? ?????????????????????????????? ???????????? ???????????????? ????????????????????????, " +
                    "?? ?????????????? ?????????????????? page - ?????????? ????????????????, ???????????????????????? ????????????????, items (???? ?????????????????? 10) - ???????????????????? ?????????????????????? ???? ????????????????")
    @ApiResponse(responseCode = "200", description = "???????????????????? ???????????????????????????? ???????????? PageDTO<QuestionDTO> (id, title, authorId," +
            " authorReputation, authorName, authorImage, description, viewCount, countAnswer, countValuable," +
            " LocalDateTime, LocalDateTime, listTagDto", content = {
            @Content(mediaType = "application/json")
    })
    public ResponseEntity<PageDTO<QuestionViewDto>> AllQuestionSortedByPopular(@RequestParam int page,
                                                                               @RequestParam(required = false, defaultValue = "10") int items,
                                                                               @RequestParam(required = false) List<Long> trackedTag,
                                                                               @RequestParam(required = false) List<Long> ignoredTag,
                                                                               @RequestParam(required = false, defaultValue = "ALL") DateFilter dateFilter,
                                                                               Authentication auth) {

        PaginationData data = new PaginationData(page, items, QuestionPageDtoDaoAllSortedByPopular.class.getSimpleName());
        User user = (User) auth.getPrincipal();
        data.getProps().put("trackedTag", trackedTag);
        data.getProps().put("ignoredTag", ignoredTag);
        data.getProps().put("userId",user.getId());
        data.getProps().put("dateFilter", dateFilter.getDay());


        return new ResponseEntity<>(questionDtoService.getPageDto(data), HttpStatus.OK);
    }

    @GetMapping("api/user/question/paginationForWeek")
    @Operation(summary = "?????????????????? ?????????????????????????????? ???????????? ???????????????? ???? ???????????? ???? ???????????????????? ??????????????,???????????????????? ?? ?????????????? " +
            "?? ???????????????????????? ?????????? trackedTag ?? ignoredTag",
            description = "?????????????????? ?????????????????????????????? ???????????? ???????????????? ???? ????????????, " +
                    "?? ?????????????? ?????????????????? page - ?????????? ????????????????, ???????????????????????? ????????????????, items (???? ?????????????????? 10) - ???????????????????? ?????????????????????? ???? ????????????????," +
                    "???? ???????????????????????? ???? ????????????, trackedTag - ???? ???????????????????????? ????????????????, ignoredTag - ???? ???????????????????????? ????????????????")
    @ApiResponse(responseCode = "200", description = "???????????????????? ???????????????????????????? ???????????? PageDTO<QuestionDTO> (id, title, authorId," +
            " authorReputation, authorName, authorImage, description, viewCount, countAnswer, countValuable," +
            " LocalDateTime, LocalDateTime, listTagDto", content = {
            @Content(mediaType = "application/json")
    })
    public ResponseEntity<PageDTO<QuestionViewDto>> paginationForTheWeek(@RequestParam int page, @RequestParam(required = false, defaultValue = "10") int items,
                                                                         @RequestParam(required = false) List<Long> trackedTag,
                                                                         @RequestParam(required = false) List<Long> ignoredTag,
                                                                         @RequestParam(required = false, defaultValue = "ALL") DateFilter dateFilter) {

        PaginationData data = new PaginationData(page, items, QuestionPageDtoDaoSortedByWeightForTheWeekImpl.class.getSimpleName());
        data.getProps().put("trackedTags", trackedTag);
        data.getProps().put("ignoredTags", ignoredTag);
        data.getProps().put("dateFilter", dateFilter.getDay());


        return new ResponseEntity<>(questionDtoService.getPageDto(data), HttpStatus.OK);

    }

    @GetMapping("api/user/question/paginationForMonth")
    @Operation(summary = "?????????????????? ?????????????????????????????? ???????????? ???????????????? ???? ?????????? ???? ?????????????????????? ???????????????????? ??????????????,??????????????," +
            "(?????????????????????)(?????????????????? ?????????????????????????? ??????????????)",
            description = "?????????????????? ?????????????????????????????? ???????????? ???????????????? ???? ??????????, " +
                    "?? ?????????????? ?????????????????? page - ?????????? ????????????????, ???????????????????????? ????????????????, items (???? ?????????????????? 10) - " +
                    "???????????????????? ?????????????????????? ???? ????????????????, ?????? ???? ?????????? ?????????????????????????? ???? ignoredTag ?? trackedTag" +
                    "(?????????????????????? ?????? ???????????????? ?? HTTP ?????????????? ??????????????)")
    @ApiResponse(responseCode = "200", description = "???????????????????? ???????????????????????????? ???????????? PageDTO<QuestionViewDTO> " +
            "(id, title, authorId," +
            " authorReputation, authorName, authorImage, description, viewCount, countAnswer, countValuable," +
            " LocalDateTime, LocalDateTime, listTagDto", content = {
            @Content(mediaType = "application/json")
    })
    public ResponseEntity<PageDTO<QuestionViewDto>> paginationForTheMonth(@RequestParam int page,
                                                                          @RequestParam(required = false, defaultValue = "10") int items,
                                                                          @RequestParam(required = false) List<Long>trackedTag,
                                                                          @RequestParam(required = false) List<Long>ignoredTag,
                                                                          @RequestParam(required = false, defaultValue = "ALL") DateFilter dateFilter){
        PaginationData data = new PaginationData(page, items, QuestionPageDtoDaoSortedByImpl.class.getSimpleName());
        data.getProps().put("trackedTags", trackedTag);
        data.getProps().put("ignoredTags", ignoredTag);
        data.getProps().put("dateFilter", dateFilter.getDay());

        return new ResponseEntity<>(questionDtoService.getPageDto(data), HttpStatus.OK);
    }

    @Operation(
            summary = "???????????????????? ?????????????? ?? ????????????????",
            description = "???????????????????? ?????????????? ?? ????????????????"
    )
    @ApiResponse(responseCode = "200", description = "???????????????? ?????????????? ??????????????????", content = {
            @Content(mediaType = "application/json")
    })
    @ApiResponse(responseCode = "400", description = "???? ?????????????????????? id ?????? ?????????????? ?????? ???????????????? ?????? ????????????????????", content = {
            @Content(mediaType = "application/json")
    })
    @ApiResponse(responseCode = "403", description = "???????????????????????? ???? ????????????????????????????????", content = {
            @Content(mediaType = "application/json")
    })
    @GetMapping("api/user/question/{id}/bookmark")
    public ResponseEntity<String> addQuestionInBookmarks(@PathVariable Long id, Authentication auth) {

        User user = (User) auth.getPrincipal();
        Optional<Question> question = questionService.getById(id);

        if (question.isPresent()) {
            bookmarksService.addQuestionInBookmarks(user, question.get());
            return new ResponseEntity<>("Bookmark successfully added", HttpStatus.OK);
        }

        return new ResponseEntity<>("There is no question with id: " + id.toString(), HttpStatus.BAD_REQUEST);
    }

    @Operation(
            summary = "???????????????????? ?????????????????????? ?? ??????????????",
            description = "???????????????????? ?????????????????????? ?? ??????????????"
    )
    @ApiResponse(responseCode = "200", description = "?????????????????????? ????????????????", content = {
            @Content(mediaType = "application/json")
    })
    @ApiResponse(responseCode = "400", description = "?????????????????????? ???? ????????????????", content = {
            @Content(mediaType = "application/json")
    })
    @PostMapping("/{id}/comment")
    public ResponseEntity<?> addCommentQuestion(@PathVariable Long id, @RequestBody String bodyComment,
                                                Authentication auth) {
        User user = (User) auth.getPrincipal();
        Optional<Question> question = questionService.getById(id);
        if (question.isEmpty()){
            return new ResponseEntity<>("There is no question " + id.toString(), HttpStatus.BAD_REQUEST);
        }
        CommentQuestion commentQuestion = new CommentQuestion(bodyComment, user);
        commentQuestion.setQuestion(question.get());
        commentQuestionService.persist(commentQuestion);
        List<QuestionCommentDto> questionCommentDtoList = questionDtoService.getQuestionByIdComment(id);
        QuestionCommentDto questionCommentDto = questionCommentDtoList.get(questionCommentDtoList.size() - 1);
        return new ResponseEntity<>(questionCommentDto, HttpStatus.OK);
    }
}


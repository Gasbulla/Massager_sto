package com.javamentor.qa.platform.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.core.api.dataset.SeedStrategy;
import com.javamentor.qa.platform.AbstractClassForDRRiderMockMVCTests;
import com.javamentor.qa.platform.dao.abstracts.model.QuestionViewedDao;
import com.javamentor.qa.platform.models.dto.AuthenticationRequest;
import com.javamentor.qa.platform.models.dto.QuestionCreateDto;
import com.javamentor.qa.platform.models.dto.TagDto;
import com.javamentor.qa.platform.models.entity.BookMarks;
import com.javamentor.qa.platform.models.entity.question.Question;
import com.javamentor.qa.platform.models.entity.user.reputation.Reputation;
import com.javamentor.qa.platform.service.abstracts.model.QuestionService;
import com.javamentor.qa.platform.service.abstracts.model.QuestionViewedService;
import com.javamentor.qa.platform.service.abstracts.model.UserService;
import com.javamentor.qa.platform.webapp.configs.JmApplication;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@SpringBootTest(classes = JmApplication.class)
public class TestQuestionResourceController extends AbstractClassForDRRiderMockMVCTests {

    @Autowired
    private MockMvc mvc;

    @Test
    @DataSet(value = {
            "dataset/testQuestionIdCommentResource/Fix/comment.yml",
            "dataset/testQuestionIdCommentResource/users.yml",
            "dataset/testQuestionIdCommentResource/commentquestion.yml",
            "dataset/testQuestionIdCommentResource/questions.yml",
            "dataset/testQuestionIdCommentResource/reputations.yml",
            "dataset/testQuestionIdCommentResource/roles.yml"
    },
            strategy = SeedStrategy.CLEAN_INSERT,
            cleanAfter = true, cleanBefore = true
    )
    // ?????????????????? ???????????? ?????? ???????????????????????? ?? ????????????????
    public void shouldGetQuestionIdComment() throws Exception {
        mockMvc.perform(get("/api/user/question/1/comment")
                        .contentType("application/json")
                        .header("Authorization", "Bearer " + getToken("test1@mail.ru", "test15")))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].questionId").value(1))
                .andExpect(jsonPath("$[0].lastRedactionDate").value("2021-12-13T23:09:52.716"))
                .andExpect(jsonPath("$[0].persistDate").value("2021-12-13T23:09:52.716"))
                .andExpect(jsonPath("$[0].text").value("Hello Test"))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].imageLink").value("photo"))
                .andExpect(jsonPath("$[0].reputation").value(100))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].questionId").value(1))
                .andExpect(jsonPath("$[1].lastRedactionDate").value("2021-12-13T23:09:52.716"))
                .andExpect(jsonPath("$[1].persistDate").value("2021-12-13T23:09:52.716"))
                .andExpect(jsonPath("$[1].text").value("Hello Test2"))
                .andExpect(jsonPath("$[1].userId").value(2))
                .andExpect(jsonPath("$[1].imageLink").value("photo"))
                .andExpect(jsonPath("$[1].reputation").value(500));
    }

    @Test
    //???????????????? ???????????? ?????????????? (DOWN_VOTE) ?? ???????????????? ?????????? ?? ?????????????????????? ??????????????: 1 ?? ???????????????????? -5
    @DataSet(cleanAfter = true, cleanBefore = true,
            value = "dataset/QuestionResourceController/data.yml",
            strategy = SeedStrategy.REFRESH)
    public void shouldReturnSetupDownVoteDownReputation() throws Exception {
        this.mockMvc.perform(post("/api/user/question/2/downVote")
                        .header("Authorization", "Bearer " + getToken("test15@mail.ru", "test15")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1")));
        Query queryValidateUserVote = entityManager.createQuery("select v from Reputation v join fetch v.question join fetch v.sender where (v.sender.id in :userId) and (v.question.id in : id )  ", Reputation.class);
        queryValidateUserVote.setParameter("userId", 15L);
        queryValidateUserVote.setParameter("id", 2L);
        Reputation reputation = (Reputation) queryValidateUserVote.getSingleResult();
        assertThat(reputation.getCount()).isEqualTo(-5);
    }

    @Test
    @DataSet(cleanAfter = true, cleanBefore = true,
            value = "dataset/QuestionResourceController/data.yml",
            strategy = SeedStrategy.REFRESH)
    //???????????????? ???? ???????????? (UP_VOTE) ?? ???????????????? ?????????? ?? ?????????????????????? ??????????????: 1 ?? ?????????????????? ?????????????????? ???? +10.
    public void shouldReturnSetupUpVoteUpReputation() throws Exception {
        this.mockMvc.perform(post("/api/user/question/1/upVote")
                        .header("Authorization", "Bearer " + getToken("test15@mail.ru", "test15")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1")));
        Query queryValidateUserVote = entityManager.createQuery("select v from Reputation v join fetch v.question join fetch v.sender where (v.sender.id in :userId) and (v.question.id in : id )  ", Reputation.class);
        queryValidateUserVote.setParameter("userId", 15L);
        queryValidateUserVote.setParameter("id", 1L);
        Reputation reputation = (Reputation) queryValidateUserVote.getSingleResult();
        assertThat(reputation.getCount()).isEqualTo(10);
    }

    @Test
    //???????????????? ???????????????? ???????????? ?????????????? (DOWN_VOTE) ?? ???????????????? ??????????: "User was voting"
    // ?????????????????? ?????????? ???? ??????????????????????.
    @DataSet(cleanAfter = true, cleanBefore = true,
            value = "dataset/QuestionResourceController/data2.yml",
            strategy = SeedStrategy.REFRESH)
    public void shouldValidateUserVoteDownVote() throws Exception {
        this.mockMvc.perform(post("/api/user/question/2/downVote")
                        .header("Authorization", "Bearer " + getToken("test15@mail.ru", "test15")))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("User was voting")));
    }

    @Test
    //???????????????? ???????????????? ???? ?????????????? (UP_VOTE) ?? ???????????????? ??????????: "User was voting"
    // ?????????????????? ?????????? ???? ??????????????????????.
    @DataSet(cleanAfter = true, cleanBefore = true,
            value = "dataset/QuestionResourceController/data2.yml",
            strategy = SeedStrategy.REFRESH)
    public void shouldValidateUserVoteUpVote() throws Exception {
        this.mockMvc.perform(post("/api/user/question/1/upVote").header("Authorization", "Bearer " + getToken("test15@mail.ru", "test15"))).andDo(print()).andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("User was voting")));
    }

    @Test
    @DataSet(value = {
            "dataset/QuestionResourceController/roles.yml",
            "dataset/testQuestionIdCommentResource/users.yml",
            "dataset/QuestionResourceController/tags.yml",
            "dataset/QuestionResourceController/questions.yml",
            "dataset/QuestionResourceController/questions_has_tag.yml",
            "dataset/QuestionResourceController/answers_for_all_questions.yml",
            "dataset/QuestionResourceController/Fix/reputations.yml",
            "dataset/QuestionResourceController/votes_on_questions.yml",
            "dataset/testQuestionIdCommentResource/comment.yml",
            "dataset/testQuestionIdCommentResource/commentquestion.yml",
            "dataset/QuestionResourceController/question_viewed/qv_viewCount1.yml"
    },
            strategy = SeedStrategy.CLEAN_INSERT,
            cleanAfter = true, cleanBefore = true
    )
    // ?????????????????? json ???? ?????????????????????????? ??????????????
    public void getCorrectQuestionDtoByIdTest() throws Exception {

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setPassword("test15");
        authenticationRequest.setUsername("test15@mail.ru");

        String USER_TOKEN = mockMvc.perform(
                        post("/api/auth/token")
                                .content(new ObjectMapper().writeValueAsString(authenticationRequest))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        USER_TOKEN = "Bearer " + USER_TOKEN.substring(USER_TOKEN.indexOf(":") + 2, USER_TOKEN.length() - 2);

        mockMvc.perform(get("/api/user/question/1")
                        .header(AUTHORIZATION, USER_TOKEN))
                .andDo(print())
                .andExpect(content().contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("test"))
                .andExpect(jsonPath("$.authorId").value(15))
                .andExpect(jsonPath("$.authorReputation").value(100))
                .andExpect(jsonPath("$.authorName").value("test 15"))
                .andExpect(jsonPath("$.authorImage").value("photo"))
                .andExpect(jsonPath("$.description").value("test"))
                .andExpect(jsonPath("$.viewCount").value(2))
                .andExpect(jsonPath("$.countAnswer").value(1))
                .andExpect(jsonPath("$.countValuable").value(-1))
                .andExpect(jsonPath("$.countAnswer").value(1))
                .andExpect(jsonPath("$.persistDateTime").value("2021-12-13T18:09:52.716"))
                .andExpect(jsonPath("$.lastUpdateDateTime").value("2021-12-13T18:09:52.716"))
                .andExpect(jsonPath("$.listTagDto[0].name").value("testNameTag"))
                .andExpect(jsonPath("$.listTagDto[0].id").value(1))
                .andExpect(jsonPath("$.listQuestionCommentDto[0].id").value(1))
                .andExpect(jsonPath("$.listQuestionCommentDto[0].lastRedactionDate")
                        .value("2021-12-13T21:09:52.716"))
                .andExpect(jsonPath("$.listQuestionCommentDto[0].persistDate")
                        .value("2021-12-13T21:09:52.716"))
                .andExpect(jsonPath("$.listQuestionCommentDto[0].text").value("Hello Test"))
                .andExpect(jsonPath("$.listQuestionCommentDto[1].id").value(2))
                .andExpect(jsonPath("$.listQuestionCommentDto[1].lastRedactionDate")
                        .value("2021-12-13T21:09:52.716"))
                .andExpect(jsonPath("$.listQuestionCommentDto[1].persistDate")
                        .value("2021-12-13T21:09:52.716"))
                .andExpect(jsonPath("$.listQuestionCommentDto[1].text").value("Hello Test2"))
                .andExpect(jsonPath("$.isUserVote").value("DOWN_VOTE"))
                .andExpect(jsonPath("$.answerDTOList[0].id").value(1));

        mockMvc.perform(get("/api/user/question/2")
                        .header(AUTHORIZATION, USER_TOKEN))
                .andDo(print())
                .andExpect(content().contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewCount").value(1))
                .andExpect(jsonPath("$.isUserVote").value(nullValue()));

        mockMvc.perform(get("/api/user/question/3")
                        .header(AUTHORIZATION, USER_TOKEN))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DataSet(value = {
            "dataset/QuestionResourceController/roles.yml",
            "dataset/QuestionResourceController/users.yml",
            "dataset/QuestionResourceController/tags.yml",
            "dataset/QuestionResourceController/questions.yml",
            "dataset/QuestionResourceController/questions_has_tag.yml",
            "dataset/QuestionResourceController/answers.yml",
            "dataset/QuestionResourceController/Fix/reputations.yml",
            "dataset/QuestionResourceController/votes_on_questions.yml"
    },
            strategy = SeedStrategy.CLEAN_INSERT,
            cleanAfter = true, cleanBefore = true
    )
    // ?????????????????? ???????????? ???? ???? ?????????????????????????? ?? ???????????????? ???????? ??????????????
    public void getWrongQuestionDtoByIdTest() throws Exception {
        mockMvc.perform(get("/api/user/question/5")
                        .header("Authorization", "Bearer " + getToken("test15@mail.ru", "test15")))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DataSet(value = {
            "dataset/QuestionResourceController/roles.yml",
            "dataset/QuestionResourceController/users.yml",
            "dataset/QuestionResourceController/questions.yml"

    },
            strategy = SeedStrategy.REFRESH,
            cleanAfter = true, cleanBefore = true
    )
    // ?????????????????? ???????????????????? ????????????????
    public void getQuestionCount() throws Exception {
        mockMvc.perform(get("/api/user/question/count")
                        .header("Authorization", "Bearer " + getToken("test15@mail.ru", "test15")))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DataSet(cleanAfter = true, cleanBefore = true,
            value = {
                    "dataset/testQuestionResourceController/question.yml",
                    "dataset/testQuestionResourceController/tag.yml",
                    "dataset/testQuestionResourceController/questions_has_tag.yml",
                    "dataset/QuestionResourceController/users.yml",
                    "dataset/testQuestionResourceController/role.yml",
                    "dataset/QuestionResourceController/votes_on_questions.yml"
            },
            strategy = SeedStrategy.CLEAN_INSERT
    )
    public void getQuestionSortedByDate() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setPassword("test15");
        authenticationRequest.setUsername("test15@mail.ru");

        QuestionCreateDto questionCreateDto = new QuestionCreateDto();
        questionCreateDto.setTitle("Title");

        String USER_TOKEN = mockMvc.perform(
                        post("/api/auth/token")
                                .content(new ObjectMapper().writeValueAsString(authenticationRequest))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        USER_TOKEN = "Bearer " + USER_TOKEN.substring(USER_TOKEN.indexOf(":") + 2, USER_TOKEN.length() - 2);

        // ?????? ?????????????????????????? ?????????????????? page
        mockMvc.perform(get("/api/user/question/new")
                        .header(AUTHORIZATION, USER_TOKEN)
                        .content(new ObjectMapper().writeValueAsString(questionCreateDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest());

        //???????????????? ???????????????????????? ???????????????????????? json, ???????????????????? items, ??????????
        mockMvc.perform(get("/api/user/question/new?page=1&trackedTag=1&ignoredTag=2&items=2")
                        .header(AUTHORIZATION, USER_TOKEN)
                        .content(new ObjectMapper().writeValueAsString(questionCreateDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].listTagDto[0].id").value(1))
                .andExpect(jsonPath("$.items[1].id").value(3))
                .andExpect(jsonPath("$.items[1].listTagDto[0].id").value(1))
                .andExpect(jsonPath("$.items.length()").value(2));

        //???????????????? 2 tracked ???????? ?? 2 ignored ????????, 1 tracked tag ?? 1 ignored ?????? ?????????????????? ?? ???? ???????????? ????????????????????
        mockMvc.perform(get("/api/user/question/new?page=1&trackedTag=1&trackedTag=2&ignoredTag=2&ignoredTag=3")
                        .header(AUTHORIZATION, USER_TOKEN)
                        .content(new ObjectMapper().writeValueAsString(questionCreateDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[1].id").value(3))
                .andExpect(jsonPath("$.items[2].id").value(5))
                .andExpect(jsonPath("$.items.length()").value(3));

    }

    @Test
    @DataSet(value = {
            "dataset/QuestionResourceController/roles.yml",
            "dataset/QuestionResourceController/users.yml",
            "dataset/QuestionResourceController/questions.yml"
    },
            strategy = SeedStrategy.REFRESH,
            cleanAfter = true, cleanBefore = true
    )
    //?????????????????? ???????? ???????????? ???? ?????????????????? ?? ?????????????? ???????????? ????????????????????????
    @Transactional(readOnly = false, isolation = Isolation.DEFAULT, propagation = Propagation.REQUIRED, rollbackFor = {Exception.class})
    public void getNotOneQuestionCount() throws Exception {
        entityManager.createQuery("UPDATE Question q set q.isDeleted=true where q.id=1L").executeUpdate();
        mockMvc.perform(get("/api/user/question/count")
                        .header("Authorization", "Bearer " + getToken("test15@mail.ru", "test15")))
                .andDo(print())
                .andExpect(status().isOk());

    }

    @Test
    @DataSet(value = {"dataset/QuestionResourceController/role.yml",
            "dataset/QuestionResourceController/user_entity.yml",
            "dataset/QuestionResourceController/tag.yml"
    },
            disableConstraints = true,
            cleanAfter = true, cleanBefore = true
    )
    void questionCreateDtoWithoutTitle() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setPassword("USER");
        authenticationRequest.setUsername("user@mail.ru");

        QuestionCreateDto questionCreateDto = new QuestionCreateDto();
        questionCreateDto.setDescription("Description");

        TagDto tagDto = new TagDto();
        tagDto.setName("Test");
        List<TagDto> listTagDto = new ArrayList<>();
        listTagDto.add(tagDto);
        questionCreateDto.setTags(listTagDto);

        String USER_TOKEN = mockMvc.perform(
                        post("/api/auth/token")
                                .content(new ObjectMapper().writeValueAsString(authenticationRequest))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        USER_TOKEN = "Bearer " + USER_TOKEN.substring(USER_TOKEN.indexOf(":") + 2, USER_TOKEN.length() - 2);

        mockMvc.perform(
                        post("/api/user/question")
                                .header(AUTHORIZATION, USER_TOKEN)
                                .content(new ObjectMapper().writeValueAsString(questionCreateDto))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DataSet(value = {"dataset/QuestionResourceController/role.yml",
            "dataset/QuestionResourceController/user_entity.yml",
            "dataset/QuestionResourceController/tag.yml"}, disableConstraints = true,
            cleanAfter = true, cleanBefore = true)
    void questionCreateDtoWithoutDescription() throws Exception {

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setPassword("USER");
        authenticationRequest.setUsername("user@mail.ru");

        QuestionCreateDto questionCreateDto = new QuestionCreateDto();
        questionCreateDto.setTitle("Title");

        TagDto tagDto = new TagDto();
        tagDto.setName("Test");
        List<TagDto> listTagDto = new ArrayList<>();
        listTagDto.add(tagDto);
        questionCreateDto.setTags(listTagDto);

        String USER_TOKEN = mockMvc.perform(
                        post("/api/auth/token")
                                .content(new ObjectMapper().writeValueAsString(authenticationRequest))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        USER_TOKEN = "Bearer " + USER_TOKEN.substring(USER_TOKEN.indexOf(":") + 2, USER_TOKEN.length() - 2);

        mockMvc.perform(
                        post("/api/user/question")
                                .header(AUTHORIZATION, USER_TOKEN)
                                .content(new ObjectMapper().writeValueAsString(questionCreateDto))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DataSet(value = {"dataset/QuestionResourceController/role.yml",
            "dataset/QuestionResourceController/user_entity.yml",
            "dataset/QuestionResourceController/tag.yml"}, disableConstraints = true,
            cleanAfter = true, cleanBefore = true)
    void questionCreateDtoWithoutTags() throws Exception {

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setPassword("USER");
        authenticationRequest.setUsername("user@mail.ru");

        QuestionCreateDto questionCreateDto = new QuestionCreateDto();
        questionCreateDto.setTitle("Title");
        questionCreateDto.setDescription("Description");

        String USER_TOKEN = mockMvc.perform(
                        post("/api/auth/token")
                                .content(new ObjectMapper().writeValueAsString(authenticationRequest))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        USER_TOKEN = "Bearer " + USER_TOKEN.substring(USER_TOKEN.indexOf(":") + 2, USER_TOKEN.length() - 2);

        mockMvc.perform(
                        post("/api/user/question")
                                .header(AUTHORIZATION, USER_TOKEN)
                                .content(new ObjectMapper().writeValueAsString(questionCreateDto))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DataSet(value = {"dataset/QuestionResourceController/role.yml",
            "dataset/QuestionResourceController/user_entity.yml",
            "dataset/QuestionResourceController/tag.yml"}, disableConstraints = true,
            cleanAfter = true, cleanBefore = true)
    void questionCreateDtoWithEmptyTitle() throws Exception {

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setPassword("USER");
        authenticationRequest.setUsername("user@mail.ru");

        QuestionCreateDto questionCreateDto = new QuestionCreateDto();
        questionCreateDto.setDescription("Description");
        questionCreateDto.setTitle("");

        TagDto tagDto = new TagDto();
        tagDto.setName("Test");
        List<TagDto> listTagDto = new ArrayList<>();
        listTagDto.add(tagDto);
        questionCreateDto.setTags(listTagDto);

        String USER_TOKEN = mockMvc.perform(
                        post("/api/auth/token")
                                .content(new ObjectMapper().writeValueAsString(authenticationRequest))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        USER_TOKEN = "Bearer " + USER_TOKEN.substring(USER_TOKEN.indexOf(":") + 2, USER_TOKEN.length() - 2);

        mockMvc.perform(
                        post("/api/user/question")
                                .header(AUTHORIZATION, USER_TOKEN)
                                .content(new ObjectMapper().writeValueAsString(questionCreateDto))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DataSet(value = {"dataset/QuestionResourceController/role.yml",
            "dataset/QuestionResourceController/user_entity.yml",
            "dataset/QuestionResourceController/tag.yml"}, disableConstraints = true,
            cleanAfter = true, cleanBefore = true)
    void questionCreateDtoWithEmptyDescription() throws Exception {

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setPassword("USER");
        authenticationRequest.setUsername("user@mail.ru");

        QuestionCreateDto questionCreateDto = new QuestionCreateDto();
        questionCreateDto.setTitle("Title");
        questionCreateDto.setDescription("");

        TagDto tagDto = new TagDto();
        tagDto.setName("Test");
        List<TagDto> listTagDto = new ArrayList<>();
        listTagDto.add(tagDto);
        questionCreateDto.setTags(listTagDto);

        String USER_TOKEN = mockMvc.perform(
                        post("/api/auth/token")
                                .content(new ObjectMapper().writeValueAsString(authenticationRequest))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        USER_TOKEN = "Bearer " + USER_TOKEN.substring(USER_TOKEN.indexOf(":") + 2, USER_TOKEN.length() - 2);

        mockMvc.perform(
                        post("/api/user/question")
                                .header(AUTHORIZATION, USER_TOKEN)
                                .content(new ObjectMapper().writeValueAsString(questionCreateDto))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DataSet(value = {"dataset/QuestionResourceController/role.yml",
            "dataset/QuestionResourceController/user_entity.yml",
            "dataset/QuestionResourceController/tag.yml"}, disableConstraints = true,
            cleanAfter = true, cleanBefore = true)
    void questionCreateDtoWithEmptyTags() throws Exception {

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setPassword("USER");
        authenticationRequest.setUsername("user@mail.ru");

        QuestionCreateDto questionCreateDto = new QuestionCreateDto();
        questionCreateDto.setTitle("Title");
        questionCreateDto.setDescription("Description");

        questionCreateDto.setTags(new ArrayList<>());

        String USER_TOKEN = mockMvc.perform(
                        post("/api/auth/token")
                                .content(new ObjectMapper().writeValueAsString(authenticationRequest))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        USER_TOKEN = "Bearer " + USER_TOKEN.substring(USER_TOKEN.indexOf(":") + 2, USER_TOKEN.length() - 2);

        mockMvc.perform(
                        post("/api/user/question")
                                .header(AUTHORIZATION, USER_TOKEN)
                                .content(new ObjectMapper().writeValueAsString(questionCreateDto))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DataSet(value = {"dataset/QuestionResourceController/role.yml",
            "dataset/QuestionResourceController/user_entity.yml",
            "dataset/QuestionResourceController/tag.yml"}, disableConstraints = true,
            cleanAfter = true, cleanBefore = true)
    void questionCreateDtoWithNameTagWhenExist() throws Exception {

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setPassword("USER");
        authenticationRequest.setUsername("user@mail.ru");

        QuestionCreateDto questionCreateDto = new QuestionCreateDto();
        questionCreateDto.setTitle("Title");
        questionCreateDto.setDescription("Description");

        TagDto tagDto = new TagDto();
        tagDto.setName("TAG100");
        List<TagDto> listTagDto = new ArrayList<>();
        listTagDto.add(tagDto);
        questionCreateDto.setTags(listTagDto);

        String USER_TOKEN = mockMvc.perform(
                        post("/api/auth/token")
                                .content(new ObjectMapper().writeValueAsString(authenticationRequest))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        USER_TOKEN = "Bearer " + USER_TOKEN.substring(USER_TOKEN.indexOf(":") + 2, USER_TOKEN.length() - 2);

        mockMvc.perform(
                        post("/api/user/question")
                                .header(AUTHORIZATION, USER_TOKEN)
                                .content(new ObjectMapper().writeValueAsString(questionCreateDto))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        String sqlCount = "select CAST(count(tag.id) as int) from Tag tag where tag.name = 'TAG100'";
        int rowCount = (int) entityManager.createQuery(sqlCount).getSingleResult();
        Assertions.assertTrue(rowCount == 1);


        String sql = "select tag.id from Tag tag where tag.name = 'TAG100'";
        Long tagId = (long) entityManager.createQuery(sql).getSingleResult();
        Assertions.assertTrue(tagId == 100L);
    }

    @Test
    @DataSet(value = {"dataset/QuestionResourceController/role.yml",
            "dataset/QuestionResourceController/user_entity.yml",
            "dataset/QuestionResourceController/tag.yml"}, disableConstraints = true,
            cleanAfter = true, cleanBefore = true)
    void questionCreateDtoWithNameTagWhenNotExist() throws Exception {

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setPassword("USER");
        authenticationRequest.setUsername("user@mail.ru");

        QuestionCreateDto questionCreateDto = new QuestionCreateDto();
        questionCreateDto.setTitle("Title");
        questionCreateDto.setDescription("Description");

        TagDto tagDto = new TagDto();
        tagDto.setName("Test");
        List<TagDto> listTagDto = new ArrayList<>();
        listTagDto.add(tagDto);
        questionCreateDto.setTags(listTagDto);

        String USER_TOKEN = mockMvc.perform(
                        post("/api/auth/token")
                                .content(new ObjectMapper().writeValueAsString(authenticationRequest))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        USER_TOKEN = "Bearer " + USER_TOKEN.substring(USER_TOKEN.indexOf(":") + 2, USER_TOKEN.length() - 2);

        mockMvc.perform(
                        post("/api/user/question")
                                .header(AUTHORIZATION, USER_TOKEN)
                                .content(new ObjectMapper().writeValueAsString(questionCreateDto))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        String sqlCount = "select CAST(count(tag.id) as int) from Tag tag where tag.name = 'Test'";
        int rowCount = (int) entityManager.createQuery(sqlCount).getSingleResult();
        Assertions.assertTrue(rowCount == 1);

    }

    @Test
    @DataSet(value = {"dataset/QuestionResourceController/role.yml",
            "dataset/QuestionResourceController/user_entity.yml",
            "dataset/QuestionResourceController/tag.yml"}, disableConstraints = true,
            cleanAfter = true, cleanBefore = true)
    void questionHasBeenCreated() throws Exception {

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setPassword("USER");
        authenticationRequest.setUsername("user@mail.ru");

        QuestionCreateDto questionCreateDto = new QuestionCreateDto();
        questionCreateDto.setTitle("questionHasBeenCreated");
        questionCreateDto.setDescription("questionHasBeenCreated");

        TagDto tagDto = new TagDto();
        tagDto.setName("Test");
        TagDto tagDto2 = new TagDto();
        tagDto2.setName("TAG100");
        TagDto tagDto3 = new TagDto();
        tagDto3.setName("TAG101");
        List<TagDto> listTagDto = new ArrayList<>();
        listTagDto.add(tagDto);
        listTagDto.add(tagDto2);
        listTagDto.add(tagDto3);
        questionCreateDto.setTags(listTagDto);

        String USER_TOKEN = mockMvc.perform(
                        post("/api/auth/token")
                                .content(new ObjectMapper().writeValueAsString(authenticationRequest))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        USER_TOKEN = "Bearer " + USER_TOKEN.substring(USER_TOKEN.indexOf(":") + 2, USER_TOKEN.length() - 2);

        String questionDtoJsonString = mockMvc.perform(
                        post("/api/user/question")
                                .header(AUTHORIZATION, USER_TOKEN)
                                .content(new ObjectMapper().writeValueAsString(questionCreateDto))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Integer id = JsonPath.read(questionDtoJsonString, "$.id");


        String sql = "select CAST(count(question.id) as int) from Question question where question.id =: questionDtoId";
        int rowCount = (int) entityManager.createQuery(sql).setParameter("questionDtoId", id.longValue()).getSingleResult();
        Assertions.assertTrue(rowCount == 1);
    }

    @Test
    @DataSet(value = {"dataset/QuestionResourceController/role.yml",
            "dataset/QuestionResourceController/user_entity.yml",
            "dataset/QuestionResourceController/tag.yml"}, disableConstraints = true,
            cleanAfter = true, cleanBefore = true)
    void questionHasBeenCreated_CheckTagList() throws Exception {

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setPassword("USER");
        authenticationRequest.setUsername("user@mail.ru");

        QuestionCreateDto questionCreateDto = new QuestionCreateDto();
        questionCreateDto.setTitle("questionHasBeenCreated_CheckTagList");
        questionCreateDto.setDescription("questionHasBeenCreated_CheckTagList");

        TagDto tagDto = new TagDto();
        tagDto.setName("Test");
        TagDto tagDto2 = new TagDto();
        tagDto2.setName("TAG100");
        TagDto tagDto3 = new TagDto();
        tagDto3.setName("TAG101");
        List<TagDto> listTagDto = new ArrayList<>();
        listTagDto.add(tagDto);
        listTagDto.add(tagDto2);
        listTagDto.add(tagDto3);
        questionCreateDto.setTags(listTagDto);

        String USER_TOKEN = mockMvc.perform(
                        post("/api/auth/token")
                                .content(new ObjectMapper().writeValueAsString(authenticationRequest))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        USER_TOKEN = "Bearer " + USER_TOKEN.substring(USER_TOKEN.indexOf(":") + 2, USER_TOKEN.length() - 2);

        String questionDtoJsonString = mockMvc.perform(
                        post("/api/user/question")
                                .header(AUTHORIZATION, USER_TOKEN)
                                .content(new ObjectMapper().writeValueAsString(questionCreateDto))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Integer id = JsonPath.read(questionDtoJsonString, "$.id");
        List<HashMap> tagListQuestionDto = JsonPath.read(questionDtoJsonString, "$.listTagDto");
        List<Integer> listId = tagListQuestionDto.stream().map(list -> (int) list.get("id")).collect(Collectors.toList());

        String sql = "select CAST(question_has_tag.tag_id as int)" +
                " from question_has_tag where question_has_tag.question_id = ?";
        List<Integer> listTag = entityManager.createNativeQuery(sql).setParameter(1, id).getResultList();

        Assertions.assertArrayEquals(listTag.toArray(), listId.toArray());
    }

    @Test
    @DataSet(value = {"dataset/QuestionResourceController/role.yml",
            "dataset/QuestionResourceController/user_entity.yml",
            "dataset/QuestionResourceController/tag.yml"}, disableConstraints = true,
            cleanAfter = true, cleanBefore = true)
    void checkFieldsQuestionInReturnedQuestionDto() throws Exception {

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setPassword("USER");
        authenticationRequest.setUsername("user@mail.ru");

        QuestionCreateDto questionCreateDto = new QuestionCreateDto();
        questionCreateDto.setTitle("checkFieldsReturnedQuestionDto");
        questionCreateDto.setDescription("checkFieldsReturnedQuestionDto");

        TagDto tagDto = new TagDto();
        tagDto.setName("Test");
        TagDto tagDto2 = new TagDto();
        tagDto2.setName("TAG100");
        TagDto tagDto3 = new TagDto();
        tagDto3.setName("TAG101");
        List<TagDto> listTagDto = new ArrayList<>();
        listTagDto.add(tagDto);
        listTagDto.add(tagDto2);
        listTagDto.add(tagDto3);
        questionCreateDto.setTags(listTagDto);

        String USER_TOKEN = mockMvc.perform(
                        post("/api/auth/token")
                                .content(new ObjectMapper().writeValueAsString(authenticationRequest))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        USER_TOKEN = "Bearer " + USER_TOKEN.substring(USER_TOKEN.indexOf(":") + 2, USER_TOKEN.length() - 2);

        String questionDtoJsonString = mockMvc.perform(
                        post("/api/user/question")
                                .header(AUTHORIZATION, USER_TOKEN)
                                .content(new ObjectMapper().writeValueAsString(questionCreateDto))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Integer id = JsonPath.read(questionDtoJsonString, "$.id");

        String sql = "select question from Question question join fetch question.user where question.id =: questionDtoId";
        Question questionFromBase = entityManager.createQuery(sql, Question.class).setParameter("questionDtoId", id.longValue()).getSingleResult();

        Assertions.assertEquals(questionFromBase.getId(), id.longValue());
        Assertions.assertEquals(questionFromBase.getTitle(),
                (JsonPath.read(questionDtoJsonString, "$.title")));
        Assertions.assertEquals(questionFromBase.getDescription(),
                (JsonPath.read(questionDtoJsonString, "$.description")));
        Assertions.assertEquals(questionFromBase.getUser().getNickname(),
                (JsonPath.read(questionDtoJsonString, "$.authorName")));
        Assertions.assertEquals(questionFromBase.getUser().getId().intValue(),
                (int) JsonPath.read(questionDtoJsonString, "$.authorId"));
        Assertions.assertEquals(questionFromBase.getUser().getImageLink(),
                (JsonPath.read(questionDtoJsonString, "$.authorImage")));
        Assertions.assertEquals(questionFromBase.getPersistDateTime().toString().substring(0, 23),
                (JsonPath.read(questionDtoJsonString, "$.persistDateTime").toString().substring(0, 23)));
        Assertions.assertEquals(questionFromBase.getLastUpdateDateTime().toString().substring(0, 23),
                (JsonPath.read(questionDtoJsonString, "$.lastUpdateDateTime").toString().substring(0, 23)));
        Assertions.assertEquals(0, (int) JsonPath.read(questionDtoJsonString, "$.viewCount"));
        Assertions.assertEquals(0, (int) JsonPath.read(questionDtoJsonString, "$.countAnswer"));
        Assertions.assertEquals(0, (int) JsonPath.read(questionDtoJsonString, "$.countValuable"));
    }

    @Test
//  ???????????????? ?????? ?????????????? ???? id ????????, ?? items ?? ??????
    @DataSet(value = {
                    "dataset/testQuestionTagIdResource/questions.yml",
                    "dataset/testQuestionTagIdResource/tag.yml",
                    "dataset/testQuestionTagIdResource/questions_has_tag.yml",
                    "dataset/testQuestionTagIdResource/reputations.yml",
                    "dataset/testQuestionTagIdResource/answers.yml",
                    "dataset/testQuestionTagIdResource/users.yml",
                    "dataset/testQuestionTagIdResource/votes_on_questions.yml",
                    "dataset/testQuestionTagIdResource/role.yml",
                    "dataset/QuestionResourceController/question_viewed/qv_viewCount2.yml"
            },
            strategy = SeedStrategy.CLEAN_INSERT,
            cleanAfter = true, cleanBefore = true)
    public void shouldReturnAllQuestionsByTagId() throws Exception {

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setPassword("test15");
        authenticationRequest.setUsername("test15@mail.ru");


        String USER_TOKEN = mockMvc.perform(
                        post("/api/auth/token")
                                .content(new ObjectMapper().writeValueAsString(authenticationRequest))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        USER_TOKEN = "Bearer " + USER_TOKEN.substring(USER_TOKEN.indexOf(":") + 2, USER_TOKEN.length() - 2);

        mockMvc.perform(get("/api/user/question/tag/100?page=1&items=2")
                        .contentType("application/json")
                        .header(AUTHORIZATION, USER_TOKEN))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.currentPageNumber").value("1"))
                .andExpect(jsonPath("$.totalPageCount").value("2"))
                .andExpect(jsonPath("$.totalResultCount").value("3"))
                .andExpect(jsonPath("$.items.length()").value("2"))

                .andExpect(jsonPath("$.items[0].id").value("100"))
                .andExpect(jsonPath("$.items[0].title").value("test1"))
                .andExpect(jsonPath("$.items[0].authorId").value("100"))
                .andExpect(jsonPath("$.items[0].authorReputation").value("100"))
                .andExpect(jsonPath("$.items[0].authorName").value("USER"))
                .andExpect(jsonPath("$.items[0].authorImage").value("image"))
                .andExpect(jsonPath("$.items[0].description").value("test1"))
                .andExpect(jsonPath("$.items[0].viewCount").value("2"))
                .andExpect(jsonPath("$.items[0].countAnswer").value("1"))
                .andExpect(jsonPath("$.items[0].countValuable").value("-1"))
                .andExpect(jsonPath("$.items[0].persistDateTime").value("2021-12-13T18:09:55"))
                .andExpect(jsonPath("$.items[0].lastUpdateDateTime").
                        value("2021-12-13T18:09:52"))
                .andExpect(jsonPath("$.items[0].listTagDto[0].name").value("TAG100"))
                .andExpect(jsonPath("$.items[0].listTagDto[0].id").value("100"))

                .andExpect(jsonPath("$.items[1].id").value("101"))
                .andExpect(jsonPath("$.items[1].title").value("test2"))
                .andExpect(jsonPath("$.items[1].authorId").value("100"))
                .andExpect(jsonPath("$.items[1].authorReputation").value("100"))
                .andExpect(jsonPath("$.items[1].authorName").value("USER"))
                .andExpect(jsonPath("$.items[1].authorImage").value("image"))
                .andExpect(jsonPath("$.items[1].description").value("test2"))
                .andExpect(jsonPath("$.items[1].viewCount").value("1"))
                .andExpect(jsonPath("$.items[1].countAnswer").value("1"))
                .andExpect(jsonPath("$.items[1].countValuable").value("-1"))
                .andExpect(jsonPath("$.items[1].persistDateTime").value("2021-12-13T18:09:54"))
                .andExpect(jsonPath("$.items[1].lastUpdateDateTime").
                        value("2021-12-13T18:09:52"))
                .andExpect(jsonPath("$.items[1].listTagDto[0].name").value("TAG100"))
                .andExpect(jsonPath("$.items[1].listTagDto[0].id").value("100"));

        mockMvc.perform(get("/api/user/question/tag/101?page=1")
                        .contentType("application/json")
                        .header(AUTHORIZATION, USER_TOKEN))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.currentPageNumber").value("1"))
                .andExpect(jsonPath("$.totalPageCount").value("1"))
                .andExpect(jsonPath("$.totalResultCount").value("10"))
                .andExpect(jsonPath("$.items.length()").value("10"));
    }

    @Test
    @DataSet(value = {
            "dataset/QuestionResourceController/users.yml",
            "dataset/QuestionResourceController/roles.yml",
            "dataset/QuestionResourceController/tag.yml",
            "dataset/QuestionResourceController/questions.yml",
            "dataset/QuestionResourceController/more_questions_has_tags.yml",
            "dataset/QuestionResourceController/answers.yml",
            "dataset/QuestionResourceController/votes_on_questions.yml",
            "dataset/QuestionResourceController/question_viewed/qv_viewCount1.yml",
            "dataset/QuestionResourceController/bookmarks.yml"
    },
            strategy = SeedStrategy.CLEAN_INSERT,
            cleanAfter = true, cleanBefore = true
    )
    // ?????????????????? json ???? ???????????????? ?????? ??????????????
    public void getCorrectListOfQuestionsWithoutAnswers() throws Exception {
        // ??????????????????, ???????????? ???? ???????????? ?????????????? ?????? ??????????????: ???????????????? ?????????????? 2 ?? 3, ?????? ?????? ???? ???????????? ???????????? ???????? ??????????,
        // ?? ???????????????? itemsOnPage = 2
        String userToken = getToken("test15@mail.ru", "test15");
        mockMvc.perform(get("/api/user/question/noAnswer?page=1&items=2")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andDo(print())
                .andExpect(jsonPath("$.items[0].viewCount").value("1"))
                .andExpect(jsonPath("$.items.[0].id").value(2))
                .andExpect(jsonPath("$.items.[1].id").value(3))
                .andExpect(jsonPath("$.items[1].viewCount").value("0"))
                .andExpect(jsonPath("$.items[0].isUserBookmark").value(false))
                .andExpect(jsonPath("$.items[1].isUserBookmark").value(true))
                .andExpect(jsonPath("$.totalResultCount").value(3))
                .andExpect(jsonPath("$.items.size()").value(2))
                .andExpect(jsonPath("$.totalPageCount").value(2));

        // ?????????????????? ?????????????????????????????????? ??????????: ???????????? 2 ????????????????, ???????????? ?????? ???????????????? trackedTag, ???????????? 3 ????????????????????,
        // ?????? ?????? ???????????????? tracked ?? ignored ????????, ???????????? 4 ????????????????????, ?????? ?????? ???????????????? ignoredTag, ???????????? 1
        // ????????????????????, ?????? ?????? ???? ???????? ?????? ??????????
        mockMvc.perform(get("/api/user/question/noAnswer?page=1&items=2&trackedTag=101&trackedTag=102&trackedTag=103&ignoredTag=103&ignoredTag=104")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andDo(print())
                .andExpect(jsonPath("$.items.[0].id").value(2))
                .andExpect(jsonPath("$.items.[0].listTagDto.[0].id").value(102))
                .andExpect(jsonPath("$.items.[0].listTagDto.[1].id").value(105))
                .andExpect(jsonPath("$.totalResultCount").value(1))
                .andExpect(jsonPath("$.items.size()").value(1))
                .andExpect(jsonPath("$.itemsOnPage").value(1))
                .andExpect(jsonPath("$.totalPageCount").value(1));


        // ?????????????????? ???????????? ?????? ?????????????????????????????? ?????????????????? itemsOnPage
        mockMvc.perform(get("/api/user/question/noAnswer?page=1")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andDo(print())
                .andExpect(jsonPath("$.items.[0].id").value(2))
                .andExpect(jsonPath("$.items.[1].id").value(3))
                .andExpect(jsonPath("$.totalResultCount").value(3))
                .andExpect(jsonPath("$.items.size()").value(3))
                .andExpect(jsonPath("$.itemsOnPage").value(3))
                .andExpect(jsonPath("$.totalPageCount").value(1));

        // ?????????????????? ???????????????????? ???? ????????????????
        mockMvc.perform(get("/api/user/question/noAnswer?page=2&items=2")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andDo(print())
                .andExpect(jsonPath("$.items.[0].id").value(4))
                .andExpect(jsonPath("$.totalResultCount").value(3))
                .andExpect(jsonPath("$.items.size()").value(1))
                .andExpect(jsonPath("$.itemsOnPage").value(1))
                .andExpect(jsonPath("$.totalPageCount").value(2));
    }

    @Test
    @DataSet(value = {
            "dataset/QuestionResourceController/users.yml",
            "dataset/QuestionResourceController/roles.yml",
            "dataset/QuestionResourceController/tag.yml",
            "dataset/QuestionResourceController/questions.yml",
            "dataset/QuestionResourceController/more_questions_has_tags.yml",
            "dataset/QuestionResourceController/answers_for_all_questions.yml",
            "dataset/QuestionResourceController/votes_on_questions.yml"
    },
            strategy = SeedStrategy.CLEAN_INSERT,
            cleanAfter = true, cleanBefore = true
    )
    // ?????????????????? json ???? ???????????????? ?????? ??????????????, ?????????? ?????? ?????????? ??????????????
    public void getQuestionsWithoutAnswersWhenThereIsNoSuchQuestions() throws Exception {

        String userToken = getToken("test15@mail.ru", "test15");
        mockMvc.perform(get("/api/user/question/noAnswer?page=1&items=2")
                        .header("Authorization", "Bearer " + userToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andDo(print())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalResultCount").value(0));
    }

    // ?????????????????? json ???? c?????????? ????????????????
    public void testAllQuestionsWithTrackedTagsAndIgnoredTags() throws Exception {

        String USER_TOKEN = getToken("test15@mail.ru", "test15");
        mockMvc.perform(get("/api/user/question?page=1&items=2")
                        .header("Authorization", "Bearer " + USER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andDo(print())
                .andExpect(jsonPath("$.items.[0].id").value(1))
                .andExpect(jsonPath("$.items.[1].id").value(2))
                .andExpect(jsonPath("$.totalResultCount").value(4));


        mockMvc.perform(get("/api/user/question?page=1")
                        .header("Authorization", "Bearer " + USER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andDo(print())
                .andExpect(jsonPath("$.items.[0].id").value(1))
                .andExpect(jsonPath("$.items.[1].id").value(2))
                .andExpect(jsonPath("$.items.[2].id").value(3))
                .andExpect(jsonPath("$.items.[3].id").value(4))
                .andExpect(jsonPath("$.totalResultCount").value(4));


        mockMvc.perform(get("/api/user/question?page=1&items=4&trackedTag=101")
                        .header("Authorization", "Bearer " + USER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andDo(print())
                .andExpect(jsonPath("$.items.[0].id").value(1))
                .andExpect(jsonPath("$.items.[0].listTagDto.[0].id").value(101))
                .andExpect(jsonPath("$.totalResultCount").value(1));

        mockMvc.perform(get("/api/user/question?page=1&items=4&trackedTag=102")
                        .header("Authorization", "Bearer " + USER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andDo(print())
                .andExpect(jsonPath("$.items.[0].id").value(2))
                .andExpect(jsonPath("$.items.[0].listTagDto.[0].id").value(102))
                .andExpect(jsonPath("$.items.[0].listTagDto.[1].id").value(105))
                .andExpect(jsonPath("$.items.[1].id").value(3))
                .andExpect(jsonPath("$.items.[1].listTagDto.[0].id").value(102))
                .andExpect(jsonPath("$.items.[1].listTagDto.[1].id").value(103))
                .andExpect(jsonPath("$.totalResultCount").value(2));

        mockMvc.perform(get("/api/user/question?page=1&items=4&ignoredTag=101")
                        .header("Authorization", "Bearer " + USER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andDo(print())
                .andExpect(jsonPath("$.items.[0].id").value(2))
                .andExpect(jsonPath("$.items.[0].listTagDto.[0].id").value(102))
                .andExpect(jsonPath("$.items.[0].listTagDto.[1].id").value(105))
                .andExpect(jsonPath("$.items.[1].id").value(3))
                .andExpect(jsonPath("$.items.[1].listTagDto.[0].id").value(102))
                .andExpect(jsonPath("$.items.[1].listTagDto.[1].id").value(103))
                .andExpect(jsonPath("$.items.[2].id").value(4))
                .andExpect(jsonPath("$.items.[2].listTagDto.[0].id").value(104))
                .andExpect(jsonPath("$.totalResultCount").value(3));

        mockMvc.perform(get("/api/user/question?page=1&items=4&ignoredTag=102")
                        .header("Authorization", "Bearer " + USER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andDo(print())
                .andExpect(jsonPath("$.items.[0].id").value(1))
                .andExpect(jsonPath("$.items.[0].listTagDto.[0].id").value(101))
                .andExpect(jsonPath("$.items.[1].id").value(4))
                .andExpect(jsonPath("$.items.[1].listTagDto.[0].id").value(104))
                .andExpect(jsonPath("$.totalResultCount").value(2));

        mockMvc.perform(get("/api/user/question?page=1&items=4&trackedTag=101&ignoredTag=102")
                        .header("Authorization", "Bearer " + USER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andDo(print())
                .andExpect(jsonPath("$.items.[0].id").value(1))
                .andExpect(jsonPath("$.items.[0].listTagDto.[0].id").value(101))
                .andExpect(jsonPath("$.totalResultCount").value(1));
    }

    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private QuestionViewedDao questionViewedDao;
    @Autowired
    private QuestionViewedService questionViewedService;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private UserService userService;

    @Test
    @DataSet(
            value = {
                    "dataset/QuestionResourceController/question_viewed/question_viewed.yml"
            },
            cleanBefore = true, cleanAfter = true,
            strategy = SeedStrategy.CLEAN_INSERT)
    @ExpectedDataSet(value = {
            "dataset/QuestionResourceController/expected/question_viewed.yml"
    },
            ignoreCols = {"persist_date"})
    public void shouldMarkQuestionLikeViewed() throws Exception {

        String token100 = "Bearer " + getToken("user100@mail.ru", "password");
        String token101 = "Bearer " + getToken("user101@mail.ru", "user101");

        //???????????????? ?????????? ????????????
        mockMvc.perform(get("/api/user/question/101/view")
                        .contentType("application/json")
                        .header("Authorization", token100))
                .andDo(print())
                .andExpect(status().isOk());

        assertNull(cacheManager.getCache("QuestionViewed").get("101user100@mail.ru"));

        //???????????????? ?????? ???? ????????????????
        mockMvc.perform(get("/api/user/question/101/view")
                        .contentType("application/json")
                        .header("Authorization", token100))
                .andDo(print())
                .andExpect(status().isOk());

        assertNotNull(cacheManager.getCache("QuestionViewed").get("101user100@mail.ru"));

        //???????????????? ?????? ???????????????????????? ???????????? ?????? user100
        mockMvc.perform(get("/api/user/question/102/view")
                        .contentType("application/json")
                        .header("Authorization", token100))
                .andDo(print())
                .andExpect(status().isOk());

        assertNotNull(cacheManager.getCache("QuestionViewed").get("102user100@mail.ru"));

        //???????????????? ???????????????????????????? ????????????
        mockMvc.perform(get("/api/user/question/1/view")
                        .contentType("application/json")
                        .header("Authorization", token100))
                .andDo(print())
                .andExpect(status().isBadRequest());

        assertNull(cacheManager.getCache("QuestionViewed").get("1user100@mail.ru"));

        questionViewedDao.getQuestionViewedByUserAndQuestion("user100@mail.ru", 1L);

        assertNotNull(cacheManager.getCache("QuestionViewed").get("1user100@mail.ru"));

        //???????????????? ?????? ???????????????????????? ???????????? ?????? user101
        mockMvc.perform(get("/api/user/question/102/view")
                        .contentType("application/json")
                        .header("Authorization", token101))
                .andDo(print())
                .andExpect(status().isOk());

        assertNotNull(cacheManager.getCache("QuestionViewed").get("102user101@mail.ru"));

    }

    @Test
    @DataSet(
            value = {
                    "dataset/QuestionResourceController/AllQuestionSortedByPopular/AllQuestionSortedByPopular.yml"
            },
            cleanBefore = true, cleanAfter = true,
            strategy = SeedStrategy.CLEAN_INSERT)
    public void getQuestionPageDtoDaoAllSortedByPopular() throws Exception {

        String token100 = "Bearer " + getToken("user100@mail.ru", "password");

        mockMvc.perform(get("/api/user/popular?page=1")
                        .contentType("application/json")
                        .header("Authorization", token100))
                .andDo(print())
                .andExpect(jsonPath("$.items.[0].id").value(101))
                .andExpect(jsonPath("$.items.[1].id").value(102))
                .andExpect(jsonPath("$.items.[2].id").value(108))
                .andExpect(jsonPath("$.items.[3].id").value(103))
                .andExpect(jsonPath("$.items.[4].id").value(104))
                .andExpect(jsonPath("$.items.[5].id").value(105))
                .andExpect(jsonPath("$.items.[6].id").value(106))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/popular?page=1&items=10&trackedTag=101,102&ignoredTag=103")
                        .contentType("application/json")
                        .header("Authorization", token100))
                .andDo(print())
                .andExpect(jsonPath("$.items.[0].id").value(101))
                .andExpect(jsonPath("$.items.[1].id").value(102))
                .andExpect(jsonPath("$.items.[2].id").value(103))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/popular?page=1&items=10&trackedTag=101,102")
                        .contentType("application/json")
                        .header("Authorization", token100))
                .andDo(print())
                .andExpect(jsonPath("$.items.[0].id").value(101))
                .andExpect(jsonPath("$.items.[1].id").value(102))
                .andExpect(jsonPath("$.items.[2].id").value(103))
                .andExpect(jsonPath("$.items.[3].id").value(104))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/popular?page=1&items=10&trackedTag=103&ignoredTag=101,102")
                        .contentType("application/json")
                        .header("Authorization", token100))
                .andDo(print())
                .andExpect(jsonPath("$.items.[0].id").value(104))
                .andExpect(status().isOk());
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    @Test
    @DataSet(value = {
                    "dataset/testQuestionResourceController/question_different_date.yml",
                    "dataset/testQuestionResourceController/tag.yml",
                    "dataset/testQuestionResourceController/answer.yml",
                    "dataset/testQuestionResourceController/questions_has_tag1.yml",
                    "dataset/QuestionResourceController/users.yml",
                    "dataset/testQuestionResourceController/role.yml",
                    "dataset/QuestionResourceController/votes_on_questions.yml"
            },
            strategy = SeedStrategy.CLEAN_INSERT,
            cleanAfter = true, cleanBefore = true
    )
    public void getQuestionSortedByWeightForTheWeek() throws Exception {

        entityManager.createNativeQuery(
                        "update question set persist_date = LOCALTIMESTAMP where id < 6"
                )
                .executeUpdate();
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setPassword("test15");
        authenticationRequest.setUsername("test15@mail.ru");

        QuestionCreateDto questionCreateDto = new QuestionCreateDto();
        questionCreateDto.setTitle("Title");

        String USER_TOKEN = mockMvc.perform(
                        post("/api/auth/token")
                                .content(new ObjectMapper().writeValueAsString(authenticationRequest))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        USER_TOKEN = "Bearer " + USER_TOKEN.substring(USER_TOKEN.indexOf(":") + 2, USER_TOKEN.length() - 2);

        // ?????? ?????????????????????????? ?????????????????? page
        mockMvc.perform(get("/api/user/question/paginationForWeek")
                        .header(AUTHORIZATION, USER_TOKEN)
                        .content(new ObjectMapper().writeValueAsString(questionCreateDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest());

        //???????????????? ???????????????????????? ???????????????????????? json, ???????????????????? items ?????? ???????? ????-???? ?????????????????????? ???? ???????? ???? ????????????
        mockMvc.perform(get("/api/user/question/paginationForWeek?page=1")
                        .header(AUTHORIZATION, USER_TOKEN)
                        .content(new ObjectMapper().writeValueAsString(questionCreateDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].listTagDto[0].id").value(1))
                .andExpect(jsonPath("$.items.length()").value(5));

        //???????????????? ???????????????????????? ???????????????????????? json, ???????????????????? items ?? ????????????????????
        mockMvc.perform(get("/api/user/question/paginationForWeek?page=1&items=2")
                        .header(AUTHORIZATION, USER_TOKEN)
                        .content(new ObjectMapper().writeValueAsString(questionCreateDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].listTagDto[0].id").value(1))
                .andExpect(jsonPath("$.items.length()").value(2));

        //???????????????? ???????????????????????? ???????????????????????? json
        mockMvc.perform(get("/api/user/question/paginationForWeek?page=1&items=4&trackedTag=4")
                        .header(AUTHORIZATION, USER_TOKEN)
                        .content(new ObjectMapper().writeValueAsString(questionCreateDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.[0].id").value(4))
                .andExpect(jsonPath("$.items.[0].listTagDto.[0].id").value(4))
                .andExpect(jsonPath("$.totalResultCount").value(1));

        //???????????????? ???????????????????????? ???????????????????????? json
        mockMvc.perform(get("/api/user/question/paginationForWeek?page=1&items=4&ignoredTag=1")
                        .header(AUTHORIZATION, USER_TOKEN)
                        .content(new ObjectMapper().writeValueAsString(questionCreateDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.[0].id").value(4))
                .andExpect(jsonPath("$.items.[0].listTagDto.[0].id").value(4))
                .andExpect(jsonPath("$.totalResultCount").value(1));

        mockMvc.perform(get("/api/user/question/paginationForWeek?page=1&items=4&ignoredTag=1&trackedTag=4")
                        .header(AUTHORIZATION, USER_TOKEN)
                        .content(new ObjectMapper().writeValueAsString(questionCreateDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.[0].id").value(4))
                .andExpect(jsonPath("$.items.[0].listTagDto.[0].id").value(4))
                .andExpect(jsonPath("$.totalResultCount").value(1));
    }

    @Test
    @DataSet(cleanBefore = true, cleanAfter = true,
            value = {
                    "dataset/QuestionResourceController/QuestionsSortedByAnswersForLastMonth/questionsDiffPersistDate.yml",
                    "dataset/QuestionResourceController/users.yml",
                    "dataset/QuestionResourceController/QuestionsSortedByAnswersForLastMonth/AnswerForSortedByQuantity.yml",
                    "dataset/QuestionResourceController/QuestionsSortedByAnswersForLastMonth/VotesOnQuestionsForSortedByQuantity.yml",
                    "dataset/testQuestionResourceController/role.yml",
                    "dataset/testQuestionResourceController/tag.yml",
                    "dataset/QuestionResourceController/QuestionsSortedByAnswersForLastMonth/QuestionsHasTag.yml"
            },
            strategy = SeedStrategy.CLEAN_INSERT
    )
    public void getQuestionSortedByAnswersQuantityForMonth() throws Exception {

        String USER_TOKEN = "Bearer " + getToken("test15@mail.ru", "test15");

        // ?????? ?????????????????????????? ?????????????????? page
        mockMvc.perform(get("/api/user/question/paginationForMonth")
                        .header(AUTHORIZATION, USER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest());
        /* ???????????????? ???? :
        1) ????????????????????????  ???????????????????? items ???? ??????????(totalResultCount=7);
        2) ???????????????????????? ???????????????????? ?????????????????? ??????????????(totalPageCount=1);
        2) ?????????????? ???????? ?????????? ?? ???????????????????????? item (???????????????????????? QuestionViewDto)-???????????????????????? HQL ??????????????;
        3) ???????????????? ???????????????????? items ???? ?????????????????? ??????????????, ?????? ???????????? ???????????????????? ??????????????
        ???? ?????????????? ????????????????(???????????????? ???? ?????????????????? ?????????????? ?? ???????????????? ????????????????)
        */
        mockMvc.perform(get("/api/user/question/paginationForMonth?page=1")
                        .header(AUTHORIZATION, USER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResultCount").value(7))
                .andExpect(jsonPath("$.totalPageCount").value(1))
                .andExpect(jsonPath("$.items.length()").value(7))
                .andExpect(jsonPath("$.items[0].id").value(11))
                .andExpect(jsonPath("$.items[0].title").value("test11"))
                .andExpect(jsonPath("$.items[0].authorId").value(15))
                .andExpect(jsonPath("$.items[0].authorReputation").value(0))
                .andExpect(jsonPath("$.items[0].authorName").value("test 15"))
                .andExpect(jsonPath("$.items[0].authorImage").value("photo"))
                .andExpect(jsonPath("$.items[0].description").value("test11"))
                .andExpect(jsonPath("$.items[0].viewCount").value(0))
                .andExpect(jsonPath("$.items[0].countAnswer").value(8))
                .andExpect(jsonPath("$.items[0].countValuable").value(-1))
                .andExpect(jsonPath("$.items[1].id").value(1))
                .andExpect(jsonPath("$.items[2].id").value(2))
                .andExpect(jsonPath("$.items[3].id").value(4))
                .andExpect(jsonPath("$.items[4].id").value(3))
                .andExpect(jsonPath("$.items[6].id").value(5));
        /* ???????????????? ???? :
        1) ???????????????????????? ?????????????? ?????? ?????????????? ???????????? ???????????????? ?? ???????????????????? items ???? ???????????????? 5;
        2) ???????????????????????? ???????????????????? ??????????????(totalResultCount=7);
        3) ???????????????????????? ???????????????????? ??????????????(totalPageCount=2);
        4) ???????????????????? items ???? ?????????????????????? ????????????????(items.length=2);
        5) ???????????????????????? ?????????????? ?????????????? ????????????????(currentPageNumber=2);
        6) ???????????????? ???????????????????? items ???? ?????????????????? ???????????? (items[1].id=11))
        */
        mockMvc.perform(get("/api/user/question/paginationForMonth?page=2&items=5")
                        .header(AUTHORIZATION, USER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResultCount").value(7))
                .andExpect(jsonPath("$.totalPageCount").value(2))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.currentPageNumber").value(2))
                .andExpect(jsonPath("$.items[1].id").value(5));
        /*???????????????? ?????????????? ?????? ?????????????????? items=0*/
        mockMvc.perform(get("/api/user/question/paginationForMonth?page=3&items=0")
                        .header(AUTHORIZATION, USER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
        //???????????????? ???????????????????????? ???????????????????????? json ?????? ???????????? ?????????????? ???? ????????(4)
        mockMvc.perform(get("/api/user/question/paginationForMonth?page=1&items=4&trackedTag=4")
                        .header(AUTHORIZATION, USER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResultCount").value(2))
                .andExpect(jsonPath("$.items[0].id").value(11))
                .andExpect(jsonPath("$.items[1].id").value(4));
        //???????????????? ???????????????????????? ???????????????????????? json ?????? ???????????? ?????????????? ???? 2?? ??????????
        mockMvc.perform(get("/api/user/question/paginationForMonth?page=1&items=4&trackedTag=4,2")
                        .header(AUTHORIZATION, USER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResultCount").value(3))
                .andExpect(jsonPath("$.items[0].id").value(11))
                .andExpect(jsonPath("$.items[1].id").value(2))
                .andExpect(jsonPath("$.items[2].id").value(4));
        //???????????????? ???????????????????????? ???????????????????????? json ?????? ???????????? ?????????????? ???? 2?? ?????????? ?? ?????????????????? 1???? ????????
        mockMvc.perform(get("/api/user/question/paginationForMonth?page=1&items=4&trackedTag=4,1&ignoredTag=2")
                        .header(AUTHORIZATION, USER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResultCount").value(6))
                .andExpect(jsonPath("$.items[0].id").value(11))
                .andExpect(jsonPath("$.items[1].id").value(1))
                .andExpect(jsonPath("$.items[2].id").value(4))
                .andExpect(jsonPath("$.items[3].id").value(3));
    }

    @Test
    @DataSet(
            value = {
                    "dataset/QuestionResourceController/Bookmarks/bookmarks.yml"
            },
            cleanBefore = true, cleanAfter = true,
            strategy = SeedStrategy.CLEAN_INSERT)
    @ExpectedDataSet(
            value = {
                    "dataset/QuestionResourceController/Bookmarks/bookmarksExp.yml"
            })
    public void testAddQuestionInBookmarks() throws Exception {

        String token100 = "Bearer " + getToken("user100@mail.ru", "password");
        String token101 = "Bearer " + getToken("user101@mail.ru", "user101");

        //???????????????????? ???????????? ?????????????? ?? ????????????????
        mockMvc.perform(get("/api/user/question/101/bookmark")
                        .contentType("application/json")
                        .header("Authorization", token100))
                .andDo(print())
                .andExpect(status().isOk());

        //?????????????????? ???????????????????? ???????? ???? ?????????????? ???????? ???? ????????????????????????
        mockMvc.perform(get("/api/user/question/101/bookmark")
                        .contentType("application/json")
                        .header("Authorization", token100))
                .andDo(print())
                .andExpect(status().isBadRequest());

        //???????????????????? ?????????????? ?????????????? ???????? ???? ????????????????????????
        mockMvc.perform(get("/api/user/question/102/bookmark")
                        .contentType("application/json")
                        .header("Authorization", token100))
                .andDo(print())
                .andExpect(status().isOk());

        //???????????????????? ?????????????????????????????? ??????????????
        mockMvc.perform(get("/api/user/question/1/bookmark")
                        .contentType("application/json")
                        .header("Authorization", token101))
                .andDo(print())
                .andExpect(status().isBadRequest());

        //???????????????????? ?????????????? ?????????????? ????????????????????????
        mockMvc.perform(get("/api/user/question/102/bookmark")
                        .contentType("application/json")
                        .header("Authorization", token101))
                .andDo(print())
                .andExpect(status().isOk());

        //???????????????? ???? - ???????????????????????? ???? ???????????? ?? ??????????, ?????????????? ?????????????? ???????? ???????????????????????????? ????????

        mockMvc.perform(get("/api/user/question/101")
                .contentType("application/json")
                .header("Authorization", token100));
        Long id = 101L;
        Long userId = 100L;
        List<BookMarks> bookMarks = (List<BookMarks>) entityManager.createQuery("select count(*) from BookMarks bm " +
                        "where bm.question.id = :id and bm.user.id=:userId ")
                .setParameter("id", id)
                .setParameter("userId", userId)
                .getResultList();
        assertThat(bookMarks).isNotNull();
    }

    @Test
    @DataSet(
            value = {
                    "dataset/testQuestionResourceController/addCommentQuestion/comments.yml"
            },
            cleanBefore = true, cleanAfter = true,
            strategy = SeedStrategy.CLEAN_INSERT)
    @ExpectedDataSet(value = {
            "dataset/testQuestionResourceController/addCommentQuestion/expected/comments.yml"
    },
            ignoreCols = {"persist_date", "last_redaction_date"}
    )

    public void testAddCommentQuestion() throws Exception {

        String token100 = "Bearer " + getToken("user100@mail.ru", "password");
        String token101 = "Bearer " + getToken("user101@mail.ru", "user101");

        //???????????????? ?????????? ?????????????????????? user 100 ???? ?????????????? 101
        mockMvc.perform(MockMvcRequestBuilders.post("/101/comment")
                        .contentType("application/json")
                        .content("Hello Test")
                        .header("Authorization", token100))
                .andDo(print())
                .andExpect(status().isOk());


        //???????????????? ?????????? ?????????????????????? user 101 ???? ?????????????? 102
        mockMvc.perform(MockMvcRequestBuilders.post("/102/comment")
                        .contentType("application/json")
                        .content("Hello Test2")
                        .header("Authorization", token101))
                .andDo(print())
                .andExpect(status().isOk());


        //???????????????? ?????????? ?????????????????????? user 101 ???? ?????????????????????????????? ?????????????? 103
        mockMvc.perform(MockMvcRequestBuilders.post("/103/comment")
                        .contentType("application/json")
                        .content("Hello Test3")
                        .header("Authorization", token101))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DataSet(value = {
            "dataset/QuestionResourceController/roles.yml",
            "dataset/QuestionResourceController/checkAnswersSort/users.yml",
            "dataset/QuestionResourceController/tags.yml",
            "dataset/QuestionResourceController/questions.yml",
            "dataset/QuestionResourceController/questions_has_tag.yml",
            "dataset/QuestionResourceController/checkAnswersSort/answers.yml",
            "dataset/QuestionResourceController/checkAnswersSort/answerVotes.yml",
            "dataset/QuestionResourceController/question_viewed/qv_viewCount1.yml"
    },
            strategy = SeedStrategy.CLEAN_INSERT,
            cleanAfter = true, cleanBefore = true
    )
    public void getSortedAnswersOnQuestionIdWhereIsHelpfulAnswerHasLessVotes() throws Exception {

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setPassword("test15");
        authenticationRequest.setUsername("test15@mail.ru");

        String USER_TOKEN = mockMvc.perform(
                        post("/api/auth/token")
                                .content(new ObjectMapper().writeValueAsString(authenticationRequest))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        USER_TOKEN = "Bearer " + USER_TOKEN.substring(USER_TOKEN.indexOf(":") + 2, USER_TOKEN.length() - 2);

        mockMvc.perform(get("/api/user/question/1")
                        .header(AUTHORIZATION, USER_TOKEN))
                .andDo(print())
                .andExpect(content().contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.viewCount").value(2))
                .andExpect(jsonPath("$.answerDTOList[0].id").value(3))
                .andExpect(jsonPath("$.answerDTOList[0].isHelpful").value(true))
                .andExpect(jsonPath("$.answerDTOList[0].countValuable").value(-3))
                .andExpect(jsonPath("$.answerDTOList[1].id").value(2))
                .andExpect(jsonPath("$.answerDTOList[1].countValuable").value(3))
                .andExpect(jsonPath("$.answerDTOList[2].id").value(1))
                .andExpect(jsonPath("$.answerDTOList[2].countValuable").value(1))
                .andExpect(jsonPath("$.answerDTOList[3].id").value(4))
                .andExpect(jsonPath("$.answerDTOList[3].countValuable").value(-1));
    }

    @Test
    @DataSet(value = {
            "dataset/QuestionResourceController/roles.yml",
            "dataset/QuestionResourceController/checkAnswersSort/users.yml",
            "dataset/QuestionResourceController/tags.yml",
            "dataset/QuestionResourceController/questions.yml",
            "dataset/QuestionResourceController/questions_has_tag.yml",
            "dataset/QuestionResourceController/checkAnswersSort/answersWithoutIsHelpful.yml",
            "dataset/QuestionResourceController/checkAnswersSort/answerVotes.yml",
            "dataset/QuestionResourceController/question_viewed/qv_viewCount1.yml"
    },
            strategy = SeedStrategy.CLEAN_INSERT,
            cleanAfter = true, cleanBefore = true
    )
    public void getSortedAnswersOnQuestionIdWhereFieldIsHelpfulIsMissing() throws Exception {

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setPassword("test15");
        authenticationRequest.setUsername("test15@mail.ru");

        String USER_TOKEN = mockMvc.perform(
                        post("/api/auth/token")
                                .content(new ObjectMapper().writeValueAsString(authenticationRequest))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        USER_TOKEN = "Bearer " + USER_TOKEN.substring(USER_TOKEN.indexOf(":") + 2, USER_TOKEN.length() - 2);

        mockMvc.perform(get("/api/user/question/1")
                        .header(AUTHORIZATION, USER_TOKEN))
                .andDo(print())
                .andExpect(content().contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.viewCount").value(2))
                .andExpect(jsonPath("$.answerDTOList[0].id").value(2))
                .andExpect(jsonPath("$.answerDTOList[0].countValuable").value(3))
                .andExpect(jsonPath("$.answerDTOList[1].id").value(1))
                .andExpect(jsonPath("$.answerDTOList[1].countValuable").value(1))
                .andExpect(jsonPath("$.answerDTOList[2].id").value(4))
                .andExpect(jsonPath("$.answerDTOList[2].countValuable").value(-1))
                .andExpect(jsonPath("$.answerDTOList[3].id").value(3))
                .andExpect(jsonPath("$.answerDTOList[3].countValuable").value(-3));
    }

    @Test
    @DataSet(value = {
            "dataset/QuestionResourceController/roles.yml",
            "dataset/QuestionResourceController/checkAnswersSort/users.yml",
            "dataset/QuestionResourceController/tags.yml",
            "dataset/QuestionResourceController/questions.yml",
            "dataset/QuestionResourceController/questions_has_tag.yml",
            "dataset/QuestionResourceController/checkAnswersSort/null_answers.yml",
            "dataset/QuestionResourceController/question_viewed/qv_viewCount1.yml"
    },
            strategy = SeedStrategy.CLEAN_INSERT,
            cleanAfter = true, cleanBefore = true
    )
    public void getNullSortedAnswersOnQuestionIdWhereAnswersIsNull() throws Exception {

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setPassword("test15");
        authenticationRequest.setUsername("test15@mail.ru");

        String USER_TOKEN = mockMvc.perform(
                        post("/api/auth/token")
                                .content(new ObjectMapper().writeValueAsString(authenticationRequest))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        USER_TOKEN = "Bearer " + USER_TOKEN.substring(USER_TOKEN.indexOf(":") + 2, USER_TOKEN.length() - 2);

        mockMvc.perform(get("/api/user/question/1")
                        .header(AUTHORIZATION, USER_TOKEN))
                .andDo(print())
                .andExpect(content().contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.viewCount").value(2))
                .andExpect(jsonPath("$.answerDTOList").isEmpty());
    }
    @Test
    @DataSet(value = {
            "dataset/QuestionResourceController/questionsWithDateFilter.yml",

    },
            strategy = SeedStrategy.CLEAN_INSERT,
            cleanAfter = true, cleanBefore = true
    )
    void getQuestionPageDtoDaoAllQuestionsWithDateFilter() throws Exception {

        mockMvc.perform(get("/api/user/question?page=1&dateFilter=ALL")
                        .header("Authorization", "Bearer " + getToken("user100@mail.ru", "password")))
                .andDo(print())
                .andExpect((content()).contentType("application/json"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].persistDateTime").value("2020-12-13T13:07:52"))
                .andExpect(jsonPath("$.items.length()").value(8))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/question?page=1&dateFilter=YEAR")
                        .header("Authorization", "Bearer " + getToken("user100@mail.ru", "password")))
                .andDo(print())
                .andExpect((content()).contentType("application/json"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].persistDateTime").value("2021-12-13T13:07:52.716"))
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/question?page=1&dateFilter=MONTH")
                        .header("Authorization", "Bearer " + getToken("user100@mail.ru", "password")))
                .andDo(print())
                .andExpect((content()).contentType("application/json"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].persistDateTime").value("2022-07-23T13:07:52.716"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/question?page=1&dateFilter=WEEK")
                .header("Authorization", "Bearer " + getToken("user100@mail.ru", "password")))
                .andDo(print())
                .andExpect((content()).contentType("application/json"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].persistDateTime").value("2022-08-14T13:07:52.716"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(status().isOk());

    }

    @Test
    @DataSet(value = {
            "dataset/QuestionResourceController/questionsWithDateFilter.yml",

    },
            strategy = SeedStrategy.CLEAN_INSERT,
            cleanAfter = true, cleanBefore = true
    )
    void getQuestionPageDtoDaoAllSortedByPopularWithDateFilter() throws Exception {

        mockMvc.perform(get("/api/user/popular?page=1&dateFilter=ALL")
                        .header("Authorization", "Bearer " + getToken("user100@mail.ru", "password")))
                .andDo(print())
                .andExpect((content()).contentType("application/json"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].persistDateTime").value("2020-12-13T13:07:52.716"))
                .andExpect(jsonPath("$.items.length()").value(8))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/popular?page=1&dateFilter=YEAR")
                        .header("Authorization", "Bearer " + getToken("user100@mail.ru", "password")))
                .andDo(print())
                .andExpect((content()).contentType("application/json"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].persistDateTime").value("2021-12-13T13:07:52.716"))
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/popular?page=1&dateFilter=MONTH")
                        .header("Authorization", "Bearer " + getToken("user100@mail.ru", "password")))
                .andDo(print())
                .andExpect((content()).contentType("application/json"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].persistDateTime").value("2022-07-23T13:07:52.716"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/popular?page=1&dateFilter=WEEK")
                        .header("Authorization", "Bearer " + getToken("user100@mail.ru", "password")))
                .andDo(print())
                .andExpect((content()).contentType("application/json"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].persistDateTime").value("2022-08-14T13:07:52.716"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(status().isOk());

    }

    @Test
    @DataSet(value = {
            "dataset/QuestionResourceController/questionsWithDateFilter1.yml",

    },
            strategy = SeedStrategy.CLEAN_INSERT,
            cleanAfter = true, cleanBefore = true
    )
    void getQuestionPageDtoDaoByNoAnswersImplWithDateFilter() throws Exception {

        mockMvc.perform(get("/api/user/question/noAnswer?page=1&dateFilter=ALL")
                        .header("Authorization", "Bearer " + getToken("user100@mail.ru", "password")))
                .andDo(print())
                .andExpect((content()).contentType("application/json"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].persistDateTime").value("2020-12-13T20:07:52"))
                .andExpect(jsonPath("$.items.length()").value(8))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/question/noAnswer?page=1&dateFilter=YEAR")
                        .header("Authorization", "Bearer " + getToken("user100@mail.ru", "password")))
                .andDo(print())
                .andExpect((content()).contentType("application/json"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].persistDateTime").value("2021-12-13T13:07:52.716"))
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/question/noAnswer?page=1&dateFilter=MONTH")
                        .header("Authorization", "Bearer " + getToken("user100@mail.ru", "password")))
                .andDo(print())
                .andExpect((content()).contentType("application/json"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].persistDateTime").value("2022-07-23T13:07:52.716"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/question/noAnswer?page=1&dateFilter=WEEK")
                        .header("Authorization", "Bearer " + getToken("user100@mail.ru", "password")))
                .andDo(print())
                .andExpect((content()).contentType("application/json"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].persistDateTime").value("2022-08-14T13:07:52.716"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(status().isOk());

    }
    @Test
    @DataSet(value = {
            "dataset/QuestionResourceController/questionsWithDateFilter.yml",

    },
            strategy = SeedStrategy.CLEAN_INSERT,
            cleanAfter = true, cleanBefore = true
    )
    void getQuestionPageDtoDaoSortedByDateWithDateFilter() throws Exception {

        mockMvc.perform(get("/api/user/question/new?page=1&dateFilter=ALL")
                        .header("Authorization", "Bearer " + getToken("user100@mail.ru", "password")))
                .andDo(print())
                .andExpect((content()).contentType("application/json"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[7].persistDateTime").value("2020-12-13T13:07:52"))
                .andExpect(jsonPath("$.items.length()").value(8))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/question/new?page=1&dateFilter=YEAR")
                        .header("Authorization", "Bearer " + getToken("user100@mail.ru", "password")))
                .andDo(print())
                .andExpect((content()).contentType("application/json"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[2].persistDateTime").value("2021-12-13T13:07:52.716"))
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/question/new?page=1&dateFilter=MONTH")
                        .header("Authorization", "Bearer " + getToken("user100@mail.ru", "password")))
                .andDo(print())
                .andExpect((content()).contentType("application/json"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[1].persistDateTime").value("2022-07-23T13:07:52.716"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/question/new?page=1&dateFilter=WEEK")
                        .header("Authorization", "Bearer " + getToken("user100@mail.ru", "password")))
                .andDo(print())
                .andExpect((content()).contentType("application/json"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].persistDateTime").value("2022-08-14T13:07:52.716"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(status().isOk());

    }

}


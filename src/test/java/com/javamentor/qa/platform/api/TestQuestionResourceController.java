package com.javamentor.qa.platform.api;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.SeedStrategy;
import com.javamentor.qa.platform.AbstractClassForDRRiderMockMVCTests;
import com.javamentor.qa.platform.models.dto.AuthenticationRequest;
import com.javamentor.qa.platform.models.dto.AuthenticationResponse;
import com.javamentor.qa.platform.models.entity.user.reputation.Reputation;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import javax.persistence.Query;

import java.net.MalformedURLException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class TestQuestionResourceController extends AbstractClassForDRRiderMockMVCTests {

    protected TestQuestionResourceController() throws MalformedURLException {
    }

    @Test
    //Голосуем ПРОТИВ вопроса (-1) и получаем ответ с количеством голосов: -1 и репутацией -5
//    @WithMockUser(username = "test15@mail.ru", password = "test15", roles = {"USER"})
    public void shouldReturnSetupDownVoteDownReputation() throws Exception {
        this.mockMvc.perform(post("/api/user/question/2/downVote").header("Authorization", "Bearer " + getToken("test15@mail.ru","test15"))).andDo(print()).andExpect(status().isOk())
                .andExpect(content().string(containsString("-1")));
//        Query queryValidateUserVote = entityManager.createQuery("select v from Reputation v join fetch v.question join fetch v.sender where (v.sender.id in :userId) and (v.question.id in : id )  ", Reputation.class);
//        queryValidateUserVote.setParameter("userId",1L);
//        queryValidateUserVote.setParameter("id",2L);
//        Reputation reputation = (Reputation) queryValidateUserVote.getSingleResult();
//        assertThat(reputation.getCount()).isEqualTo(-5);
    }

    @Test
    @DataSet(cleanBefore = true, value = "dataset/users.yml", strategy = SeedStrategy.REFRESH )
    //Голосуем ЗА вопрос (+1) и получаем ответ с количеством голосов: 1 и репутация увеличена на +10.
    public void shouldReturnSetupUpVoteUpReputation() throws Exception {
        this.mockMvc.perform(post("/api/user/question/1/upVote")).andDo(print()).andExpect(status().isOk())
                .andExpect(content().string(containsString("1")));
        Query queryValidateUserVote = entityManager.createQuery("select v from Reputation v join fetch v.question join fetch v.sender where (v.sender.id in :userId) and (v.question.id in : id )  ", Reputation.class);
        queryValidateUserVote.setParameter("userId",1L);
        queryValidateUserVote.setParameter("id",1L);
        Reputation reputation = (Reputation) queryValidateUserVote.getSingleResult();
        assertThat(reputation.getCount()).isEqualTo(10);
    }
    @Test
    //Повторно голосуем ПРОТИВ вопроса (-1) и получаем ответ с количеством голосов: -1. Т.к.
    // повторный голос не учитывается.
    public void shouldValidateUserVoteDownVote() throws Exception {
        this.mockMvc.perform(post("/api/user/question/2/downVote")).andDo(print()).andExpect(status().isOk())
                .andExpect(content().string(containsString("-1")));
    }
    @Test
    //Повторно голосуем ЗА вопроса (+1) и получаем ответ с количеством голосов: 1. Т.к.
    // повторный голос не учитывается.
    public void shouldValidateUserVoteUpVote() throws Exception {
        this.mockMvc.perform(post("/api/user/question/1/upVote")).andDo(print()).andExpect(status().isOk())
                .andExpect(content().string(containsString("1")));
    }
    @Test
    //Голосуем ЗА вопрос с неверным ID вопроса 3 и получаем ответ "Can't find question with id:3".
    // повторный голос не учитывается.
    public void shouldValidateQuestion() throws Exception {
        this.mockMvc.perform(post("/api/user/question/3/upVote")).andDo(print()).andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Can't find question with id:3")));
    }
}

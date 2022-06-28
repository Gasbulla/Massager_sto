package com.javamentor.qa.platform.dao.impl.model.model;

import com.javamentor.qa.platform.models.entity.question.VoteQuestion;

public interface VoteQuestionDao extends ReadWriteDao<VoteQuestion, Long>{

    boolean isUserNotVoteByQuestionIdAndUserId(Long id, Long userId);
    Long getVoteByQuestionId(Long questionId);
}

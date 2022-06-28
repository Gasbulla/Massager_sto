package com.javamentor.qa.platform.dao.impl.model.model;

import com.javamentor.qa.platform.models.entity.question.answer.VoteAnswer;

public interface VoteAnswerDao extends ReadWriteDao<VoteAnswer, Long> {
    long getVoteCount(long answerId);
    boolean existsVoteByAnswerAndUser(long answerId, long currentUserId);
}

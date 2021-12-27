package com.javamentor.qa.platform.dao.abstracts.model;

import com.javamentor.qa.platform.models.entity.question.Question;

import java.util.List;

public interface QuestionDao extends ReadWriteDao<Question, Long> {

    Optional<Question> getQuestionByIdWithAuthor (Long id);

}

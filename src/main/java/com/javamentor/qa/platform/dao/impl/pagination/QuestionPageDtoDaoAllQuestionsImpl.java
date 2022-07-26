package com.javamentor.qa.platform.dao.impl.pagination;


import com.javamentor.qa.platform.dao.abstracts.pagination.PageDtoDao;
import com.javamentor.qa.platform.dao.impl.pagination.transformer.QuestionPageDtoResultTransformer;
import com.javamentor.qa.platform.models.dto.QuestionViewDto;
import com.javamentor.qa.platform.models.entity.pagination.PaginationData;
import com.javamentor.qa.platform.models.entity.question.DateFilter;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;

@Repository("QuestionPageDtoDaoAllQuestionsImpl")
public class QuestionPageDtoDaoAllQuestionsImpl implements PageDtoDao<QuestionViewDto> {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<QuestionViewDto> getPaginationItems(PaginationData properties) {

        int itemsOnPage = properties.getItemsOnPage();
        int offset = (properties.getCurrentPage() - 1) * itemsOnPage;
        return (List<QuestionViewDto>) entityManager.createQuery(
                "SELECT DISTINCT " +
                        "question.id, " +
                        "question.title, " +
                        "user.id, " +
                        "user.fullName, " +
                        "user.imageLink, " +
                        "question.description, " +
                        "question.persistDateTime," +
                        "question.lastUpdateDateTime," +

                        "(SELECT SUM(reputation.count) from Reputation reputation WHERE reputation.author.id = question.user.id), " +
                        "(coalesce((select count(answer.id) from Answer answer where answer.question.id = question.id),0)) as answerCounter, " +
                        "(coalesce((select sum(case when v.vote = 'UP_VOTE' then 1 else -1 end) from VoteQuestion v where v.question.id = question.id), 0))," +
                        "(select count(bookmarks.id) from BookMarks bookmarks where bookmarks.question.id= question.id and bookmarks.user.id=:userId ) " +

                        "from Question question " +

                        "JOIN User user on question.user.id = user.id " +

                        "where " +
                        "((:trackedTags) IS NULL OR question.id IN (select question.id from Question question join question.tags t where t.id in (:trackedTags))) and " +
                        "((:ignoredTags) IS NULL OR question.id not IN (select question.id from Question question join question.tags t where t.id in (:ignoredTags))) and " +
                        " :dateFilter = 0  OR question.persistDateTime > current_date - :dateFilter ")

                .setParameter("trackedTags", properties.getProps().get("trackedTags"))
                .setParameter("ignoredTags", properties.getProps().get("ignoredTags"))
                .setParameter("userId",properties.getProps().get("userId"))
                .setParameter("dateFilter", properties.getProps().get("dateFilter"))
                .setFirstResult(offset)
                .setMaxResults(itemsOnPage)
                .unwrap(org.hibernate.query.Query.class)
                .setResultTransformer(new QuestionPageDtoResultTransformer())
                .list();

    }
    @Override
    public Long getTotalResultCount(Map<String, Object> properties) {
        return (Long) entityManager.createQuery("select distinct count(distinct question.id) from Question question join question.tags t WHERE " +
                        "((:trackedTags) IS NULL OR t.id IN (:trackedTags)) AND" +
                        "((:ignoredTags) IS NULL OR question.id NOT IN (SELECT question.id FROM Question question JOIN question.tags t WHERE t.id IN (:ignoredTags)))")
                .setParameter("trackedTags", properties.get("trackedTags"))
                .setParameter("ignoredTags", properties.get("ignoredTags"))
                .getSingleResult();
    }
}

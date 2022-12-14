package com.javamentor.qa.platform.service.abstracts.model;

import com.javamentor.qa.platform.models.entity.question.IgnoredTag;

public interface IgnoredTagService extends ReadWriteService<IgnoredTag, Long> {

    void deleteIgnoredTagByTagIdAndUserId(Long tagId, Long userId);

    boolean existsByTagIdAndUserId(Long tagId, Long userId);
}

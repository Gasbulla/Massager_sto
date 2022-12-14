package com.javamentor.qa.platform.webapp.converters;

import com.javamentor.qa.platform.models.dto.TagDto;
import com.javamentor.qa.platform.models.entity.question.Tag;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Mapper(componentModel = "spring")
@Component
public abstract class TagConverter {

    public abstract TagDto tagToTagDto(Tag tag);

    public abstract Tag tagDtoToTag(TagDto tagDto);

    public abstract List<Tag> listTagDtoToListTag(List<TagDto> listTagDto);
}

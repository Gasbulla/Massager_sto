package com.javamentor.qa.platform.webapp.converters;

import com.javamentor.qa.platform.models.dto.UserDto;
import com.javamentor.qa.platform.models.entity.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;

@Mapper
@Component
public abstract class UserToUserDtoConverter {

    public static UserToUserDtoConverter INSTANCE = Mappers.getMapper( UserToUserDtoConverter.class );

    @Mapping(source = "user.id", target = "id")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.fullName", target = "fullName")
    @Mapping(source = "user.imageLink", target = "imageLink")
    @Mapping(source = "user.city", target = "city")
    public abstract UserDto userToUserDto(User user);

    @Named("reputation")
    public int setReputation(Long reputation) {
        return reputation.intValue();
    }
}

package com.tracenest.backend.User.mapper;

import com.tracenest.backend.User.dto.UserDto;
import com.tracenest.backend.User.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    UserDto userToUserDto(User user);
    
    User userDtoToUser(UserDto userDto);
}

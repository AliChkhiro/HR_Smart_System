package com.apprh.backend.users.infrastructure;

import com.apprh.backend.users.api.UserResponse;
import com.apprh.backend.users.domain.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}

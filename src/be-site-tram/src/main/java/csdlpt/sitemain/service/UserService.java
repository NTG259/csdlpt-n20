package csdlpt.sitemain.service;

import csdlpt.sitemain.dto.response.UserProfileResponse;
import java.util.UUID;

public interface UserService {

    UserProfileResponse getProfile(UUID maND);
}

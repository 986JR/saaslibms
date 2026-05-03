package com.saas.libms.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private UserSummaryDto user;

    public static LoginResponse of(String accessToken, UserSummaryDto user) {
        return new LoginResponse(accessToken, "Bearer", user);
    }

}

package org.andywang.wildpointer.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LoginResponse {
    private String token;
    private Integer userId;
    private Integer id;
    private String username;
    private String nickname;
    private String email;
    private String avatar;
    private String bio;
    private Integer defaultDistance;
    private Integer defaultDuration;
    private LocalDateTime createdAt;
}

package org.andywang.wildpointer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterResponse {
    private Integer userId;
    private String token;
}

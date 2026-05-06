package org.andywang.wildpointer.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.Size;

@Data
public class UpdateProfileRequest {
    @Size(max = 50, message = "昵称不能超过50个字符")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(max = 500, message = "个人简介不能超过500个字符")
    private String bio;
}

package org.andywang.wildpointer.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class AddFavoriteRequest {
    @NotBlank(message = "收藏名称不能为空")
    @Size(max = 200, message = "收藏名称不能超过200个字符")
    private String name;

    @Size(max = 500, message = "位置信息过长")
    private String location;

    @Size(max = 50, message = "类型格式不正确")
    private String type;
}

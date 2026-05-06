package org.andywang.wildpointer.dto;

import lombok.Data;

import javax.validation.constraints.Min;

@Data
public class UpdatePreferencesRequest {
    @Min(value = 1, message = "默认距离必须大于0")
    private Integer defaultDistance;

    @Min(value = 1, message = "默认时长必须大于0")
    private Integer defaultDuration;
}

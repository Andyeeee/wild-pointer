package org.andywang.wildpointer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddFavoriteResponse {
    private Integer favoriteId;
}

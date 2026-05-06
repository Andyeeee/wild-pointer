package org.andywang.wildpointer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FogCellResponse {
    private int gridLat;
    private int gridLon;
}

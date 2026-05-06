package org.andywang.wildpointer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GpxUploadResponse {
    private int totalPoints;
    private int uniqueCells;
    private int newCellsAdded;
}

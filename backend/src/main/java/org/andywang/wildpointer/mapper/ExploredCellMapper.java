package org.andywang.wildpointer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.andywang.wildpointer.entity.ExploredCell;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ExploredCellMapper extends BaseMapper<ExploredCell> {

    @Insert("<script>INSERT IGNORE INTO explored_cells (user_id, grid_lat, grid_lon, created_at) VALUES " +
            "<foreach collection='cells' item='c' separator=','>" +
            "(#{c.userId}, #{c.gridLat}, #{c.gridLon}, NOW())" +
            "</foreach></script>")
    int batchInsertIgnore(@Param("cells") List<ExploredCell> cells);
}

package com.shika.item.mapper;

import com.shika.item.pojo.Category;
import tk.mybatis.mapper.additional.idlist.IdListMapper;
import tk.mybatis.mapper.common.Mapper;

/**
 * Created by Jiang on 2019/6/21.
 */
public interface CategoryMapper extends Mapper<Category>, IdListMapper<Category,Long>{
}

package com.shika.item.service;

import com.shika.common.enums.ExceptionEnum;
import com.shika.common.exceptions.SkException;
import com.shika.item.mapper.CategoryMapper;
import com.shika.item.pojo.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Created by Jiang on 2019/6/21.
 */

/**
 * 根据parentId查询商品分类
 */
@Service
public class CategoryService {
    @Autowired
    private CategoryMapper categoryMapper;
    public List<Category> queryCategoryListByPid(long pid) {
        Category t = new Category();
        t.setParentId(pid);
        //mapper会把对象的非空字段作为查询条件
        List<Category> list = categoryMapper.select(t);
        if(CollectionUtils.isEmpty(list)){
            throw new SkException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }
        return list;
    }
}

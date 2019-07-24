package com.shika.item.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.shika.common.enums.ExceptionEnum;
import com.shika.common.exceptions.SkException;
import com.shika.common.viewObjects.PageResult;
import com.shika.item.mapper.BrandMapper;
import com.shika.item.pojo.Brand;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

/**
 * Created by Jiang on 2019/6/24.
 */
@Service
public class BrandService {

    @Autowired
    private BrandMapper brandMapper;

    public PageResult<Brand> queryBrandByPageAndSort(Integer page
            , Integer rows, String sortBy, Boolean desc, String key) {
        // 开始分页（当前页，记录数）（自动添加limit子句）
        PageHelper.startPage(page, rows);
        // 过滤，利用反射获取pojo类表名、主键等
        Example example = new Example(Brand.class);
        if (StringUtils.isNotBlank(key)) {
            example.createCriteria().orLike("name", "%" + key + "%")
                    .orEqualTo("letter", key);
        }
        if (StringUtils.isNotBlank(sortBy)) {
            // 排序,排序子句
            String orderByClause = sortBy + (desc ? " DESC" : " ASC");
            example.setOrderByClause(orderByClause);
        }
        // 查询,查询结果已包含总页数、导航页等信息
        List<Brand> list = brandMapper.selectByExample(example);
        if(CollectionUtils.isEmpty(list)){
            throw new SkException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        //解析分页结果
        PageInfo<Brand> pageInfo = new PageInfo<>(list);
        // 返回结果
        return new PageResult<>(pageInfo.getTotal(), list);
    }

    //新增品牌
    @Transactional
    public void saveBrand(Brand brand, List<Long> cids) {
        //新增品牌
        brand.setId(null);
        int cnt = brandMapper.insert(brand);
        if(cnt != 1){
            throw new SkException(ExceptionEnum.BRAND_CREATE_FAILED);
        }
        //新增中间表categoryBrand
        //brand.getId()可以，已经回显
        for (Long cid : cids) {
            cnt = brandMapper.insertCategoryBrand(cid, brand.getId());
            if(cnt != 1){
                throw new SkException(ExceptionEnum.BRAND_CREATE_FAILED);
            }
        }
    }

    public Brand queryByID(Long id){
        Brand brand = brandMapper.selectByPrimaryKey(id);
        if(brand == null){
            throw new SkException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return brand;
    }

    public List<Brand> queryBrandsByCid(Long cid) {
        List<Brand> brands = brandMapper.queryByCategoryId(cid);
        if(CollectionUtils.isEmpty(brands)){
            throw new SkException(ExceptionEnum.BRAND_NOT_FOUND);
        }else {
            return brands;
        }
    }

    public List<Brand> queryByIds(List<Long> ids) {
        List<Brand> brands = brandMapper.selectByIdList(ids);
        if(CollectionUtils.isEmpty(brands)){
            throw new SkException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return brands;
    }
}

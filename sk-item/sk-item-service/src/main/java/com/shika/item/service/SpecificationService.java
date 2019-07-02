package com.shika.item.service;

import com.shika.common.enums.ExceptionEnum;
import com.shika.common.exceptions.SkException;
import com.shika.item.mapper.SpecGroupMapper;
import com.shika.item.mapper.SpecParamMapper;
import com.shika.item.pojo.SpecGroup;
import com.shika.item.pojo.SpecParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
public class SpecificationService {

    @Autowired
    private SpecGroupMapper specGroupMapper;
    @Autowired
    private SpecParamMapper specParamMapper;

    public List<SpecGroup> queryGroupByCid(Long cid) {
        SpecGroup specGroup = new SpecGroup();
        specGroup.setCid(cid);
        List<SpecGroup> list = specGroupMapper.select(specGroup);
        if(CollectionUtils.isEmpty(list)){
            throw new SkException(ExceptionEnum.SPEC_GROUP_NOT_FOUND);
        }else {
            return list;
        }
    }

    public List<SpecParam> queryParamList(Long gid, Long cid, Boolean searching) {
        SpecParam specParam = new SpecParam();
        specParam.setGroupId(gid);
        specParam.setCid(cid);
        specParam.setSearching(searching);
        List<SpecParam> paramList = specParamMapper.select(specParam);
        if(CollectionUtils.isEmpty(paramList)){
            throw new SkException(ExceptionEnum.SPEC_PARAM_NOT_FOUND);
        }else {
            return paramList;
        }
    }
}

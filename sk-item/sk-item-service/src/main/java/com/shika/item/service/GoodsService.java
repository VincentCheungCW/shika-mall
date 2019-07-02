package com.shika.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.shika.common.enums.ExceptionEnum;
import com.shika.common.exceptions.SkException;
import com.shika.common.viewObjects.PageResult;
import com.shika.item.mapper.SkuMapper;
import com.shika.item.mapper.SpuDetailMapper;
import com.shika.item.mapper.SpuMapper;
import com.shika.item.mapper.StockMapper;
import com.shika.item.pojo.*;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.weaver.ast.Var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GoodsService {
    @Autowired
    private SpuMapper spuMapper;
    @Autowired
    private SpuDetailMapper spuDetailMapper;
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private StockMapper stockMapper;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private BrandService brandService;

    public PageResult<Spu> querySpuByPage(Integer page, Integer rows, Boolean saleable, String key) {
        //分页
        PageHelper.startPage(page, rows);
        //过滤
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        //搜索字段过滤
        if (StringUtils.isNotBlank(key)) {
            criteria.andLike("title", "%" + key + "%");
        }
        //上下架过滤
        if (saleable != null) {
            criteria.andEqualTo("saleable", saleable);
        }
        //默认排序
        example.setOrderByClause("last_update_time DESC");
        //查询
        List<Spu> spuList = spuMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(spuList)) {
            throw new SkException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        //解析分类和品牌名称
        loadCategoryAndBrandName(spuList);
        //解析分页结果
        PageInfo<Spu> spuPageInfo = new PageInfo<>(spuList);
        return new PageResult<>(spuPageInfo.getTotal(), spuList);
    }

    private void loadCategoryAndBrandName(List<Spu> spuList) {
        for (Spu spu : spuList) {
            //处理分类名称,使用Lamda表达式
            List<String> names = categoryService.queryByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()))
                    .stream().map(category -> category.getName()).collect(Collectors.toList());
            spu.setCname(StringUtils.join(names, "/"));
            //处理品牌名称
            Brand brand = brandService.queryByID(spu.getBrandId());
            spu.setBname(brand.getName());
        }
    }

    @Transactional
    public void saveGoods(Spu spu) {
        //新站spu
        spu.setId(null);
        spu.setCreateTime(new Date());
        spu.setLastUpdateTime(spu.getCreateTime());
        spu.setSaleable(Boolean.TRUE);
        spu.setValid(Boolean.TRUE);
        int cnt = spuMapper.insert(spu);
        if (cnt != 1) {
            throw new SkException(ExceptionEnum.GOODS_SAVE_ERROR);
        }
        //新站spu_detail
        SpuDetail spuDetail = spu.getSpuDetail();
        spuDetail.setSpuId(spu.getId()); //上面spu新增完成已产生id
        spuDetailMapper.insert(spuDetail);
        saveSkuAndStock(spu);
    }

    //新增sku和库存
    private void saveSkuAndStock(Spu spu) {
        int cnt;//批量插入
        List<Stock> stockList = new ArrayList<>();
        //新增sku
        List<Sku> skus = spu.getSkus();
        for (Sku sku : skus) {
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            sku.setSpuId(spu.getId());
            cnt = skuMapper.insert(sku); //不能批量新增，因为下面stock要获取sku新增后生成的id
            if (cnt != 1) {
                throw new SkException(ExceptionEnum.GOODS_SAVE_ERROR);
            }
            //新增库存
            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            stockList.add(stock);
        }
        //批量插入
        cnt = stockMapper.insertList(stockList);
        if(cnt != stockList.size()){
            throw new SkException(ExceptionEnum.GOODS_SAVE_ERROR);
        }
    }

    public SpuDetail queryDetailById(Long spuId) {
        SpuDetail spuDetail = spuDetailMapper.selectByPrimaryKey(spuId);
        if(spuDetail == null){
            throw new SkException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        return spuDetail;
    }

    public List<Sku> querySkuBySpuId(Long spuId) {
        //查询sku
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        List<Sku> skuList = skuMapper.select(sku);
        if(CollectionUtils.isEmpty(skuList)){
            throw new SkException(ExceptionEnum.SKU_NOT_FOUND);
        }
        //查询库存,Java8 流API
        List<Long> ids = skuList.stream().map(Sku::getId).collect(Collectors.toList());
        List<Stock> stockList = stockMapper.selectByIdList(ids);
        if(CollectionUtils.isEmpty(stockList)){
            throw new SkException(ExceptionEnum.STOCK_NOT_FOUND);
        }
        //把stockList变为stockMap<skuId,stock>
        Map<Long, Integer> stockMap = stockList.stream()
                .collect(Collectors.toMap(Stock::getSkuId, Stock::getStock));
        skuList.forEach(s -> s.setStock(stockMap.get(s.getId())));
        return skuList;
    }

    @Transactional
    public void updateGoods(Spu spu) {
        if(spu.getId() == null){
            throw new SkException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        //删除sku和stock
        Sku sku = new Sku();
        sku.setSpuId(spu.getId());
        List<Sku> skuList = skuMapper.select(sku);
        if(!CollectionUtils.isEmpty(skuList)){
            skuMapper.delete(sku);
            List<Long> skuIds = skuList.stream().map(Sku::getId).collect(Collectors.toList());
            stockMapper.deleteByIdList(skuIds);
        }
        //更新spu
        spu.setLastUpdateTime(new Date());
        spu.setCreateTime(null);
        spu.setValid(null);
        spu.setSaleable(null);
        int cnt = this.spuMapper.updateByPrimaryKeySelective(spu);
        if(cnt != 1){
            throw new SkException(ExceptionEnum.GOODS_UPDATE_ERROR);
        }
        //修改detail
        cnt = spuDetailMapper.updateByPrimaryKeySelective(spu.getSpuDetail());
        if(cnt != 1){
            throw new SkException(ExceptionEnum.GOODS_UPDATE_ERROR);
        }
        //新增sku和stock
        saveSkuAndStock(spu);
    }
}

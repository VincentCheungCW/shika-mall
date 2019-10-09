package com.shika.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.shika.common.dto.CartDto;
import com.shika.common.enums.ExceptionEnum;
import com.shika.common.exceptions.SkException;
import com.shika.common.viewObjects.PageResult;
import com.shika.item.mapper.SkuMapper;
import com.shika.item.mapper.SpuDetailMapper;
import com.shika.item.mapper.SpuMapper;
import com.shika.item.mapper.StockMapper;
import com.shika.item.pojo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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
    @Autowired
    private AmqpTemplate amqpTemplate;

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
        //新增sku和库存
        saveSkuAndStock(spu);
        //发送RabbitMQ消息
        try {
            amqpTemplate.convertAndSend("item.insert", spu.getId()); //item.update为routingKey,exchangeName已在配置文件中设置
        } catch (AmqpException e) {
            log.error("{}商品消息发送异常，商品id：{}", "item.insert", spu.getId(), e);
        }
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
        if (cnt != stockList.size()) {
            throw new SkException(ExceptionEnum.GOODS_SAVE_ERROR);
        }
    }

    public SpuDetail queryDetailById(Long spuId) {
        SpuDetail spuDetail = spuDetailMapper.selectByPrimaryKey(spuId);
        if (spuDetail == null) {
            throw new SkException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        return spuDetail;
    }

    public List<Sku> querySkuBySpuId(Long spuId) {
        //查询sku
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        List<Sku> skuList = skuMapper.select(sku);
        if (CollectionUtils.isEmpty(skuList)) {
            throw new SkException(ExceptionEnum.SKU_NOT_FOUND);
        }
        List<Long> ids = skuList.stream().map(Sku::getId).collect(Collectors.toList());
        loadStockInSku(ids, skuList);
        return skuList;
    }

    @Transactional
    public void updateGoods(Spu spu) {
        if (spu.getId() == null) {
            throw new SkException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        //删除sku和stock
        Sku sku = new Sku();
        sku.setSpuId(spu.getId());
        List<Sku> skuList = skuMapper.select(sku);
        if (!CollectionUtils.isEmpty(skuList)) {
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
        if (cnt != 1) {
            throw new SkException(ExceptionEnum.GOODS_UPDATE_ERROR);
        }
        //修改detail
        cnt = spuDetailMapper.updateByPrimaryKeySelective(spu.getSpuDetail());
        if (cnt != 1) {
            throw new SkException(ExceptionEnum.GOODS_UPDATE_ERROR);
        }
        //新增sku和stock
        saveSkuAndStock(spu);
        //发送RabbitMQ消息
        try {
            amqpTemplate.convertAndSend("item.update", spu.getId()); //item.update为routingKey,exchangeName已在配置文件中设置
        } catch (AmqpException e) {
            log.error("{}商品消息发送异常，商品id：{}", "item.insert", spu.getId(), e);
        }
    }

    public Spu querySpuById(Long id) {
        //查询Spu
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if (spu == null) {
            throw new SkException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        //查询Sku
        List<Sku> skus = querySkuBySpuId(id);
        spu.setSkus(skus);
        //查询detail
        SpuDetail spuDetail = queryDetailById(id);
        spu.setSpuDetail(spuDetail);
        return spu;
    }

    public List<Sku> queryskuByIds(List<Long> ids) {
        List<Sku> skus = skuMapper.selectByIdList(ids);
        if(CollectionUtils.isEmpty(skus)){
            throw new SkException(ExceptionEnum.SKU_NOT_FOUND);
        }
        loadStockInSku(ids, skus);
        return skus;
    }

    private void loadStockInSku(List<Long> ids, List<Sku> skus) {
        //查询库存,Java8 流API
        List<Stock> stockList = stockMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(stockList)) {
            throw new SkException(ExceptionEnum.STOCK_NOT_FOUND);
        }
        //把stockList变为stockMap<skuId,stock>
        Map<Long, Integer> stockMap = stockList.stream()
                .collect(Collectors.toMap(Stock::getSkuId, Stock::getStock));
        skus.forEach(s -> s.setStock(stockMap.get(s.getId())));
    }

    public Sku querySkuById(Long id) {
        return this.skuMapper.selectByPrimaryKey(id);
    }


    // 典型问题：订单提交后减库存，存在线程安全问题，产生超售，两种解决思路：
    // 1.悲观锁，先查询库存，判断库存充足再减库存，采用分布式锁（zk/redis）
    // 2.乐观锁，利用SQL语句实现，加where条件，只有库存不小于订单量，才执行减库存
    // update tb_stock set stock = stock - cartDto.getNum()
    // where sku_id = cartDto.getSkuId() and stock >= cartDto.getNum()
    @Transactional
    public void decreaseStock(List<CartDto> cartDtos) {
        for (CartDto cartDto : cartDtos) {
            int count = stockMapper.decreaseStock(cartDto.getSkuId(), cartDto.getNum());
            if (count != 1) {
                throw new SkException(ExceptionEnum.STOCK_NOT_ENOUGH);
            }
        }
    }
}

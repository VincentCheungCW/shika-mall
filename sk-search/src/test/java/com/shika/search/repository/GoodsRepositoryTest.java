package com.shika.search.repository;

import com.shika.SkSearchApplication;
import com.shika.common.viewObjects.PageResult;
import com.shika.item.pojo.Spu;
import com.shika.search.client.GoodsClient;
import com.shika.search.pojo.Goods;
import com.shika.search.service.SearchService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SkSearchApplication.class)
public class GoodsRepositoryTest {
    @Autowired
    private GoodsRepository goodsRepository;
    @Autowired
    private ElasticsearchTemplate template;
    @Autowired
    private GoodsClient goodsClient;
    @Autowired
    private SearchService searchService;

    /**
     * 创建ElasticSearch索引库,映射关系。也可以在Kibana完成
     */
    @Test
    public void testCreateIndex() {
        template.createIndex(Goods.class);
        template.putMapping(Goods.class);
    }

    /**
     * 循环查询Spu，然后调用SearchService中的方法，把SPU变为Goods，然后写入索引库
     */
    @Test
    public void loadData() {
        // 创建索引
        this.template.createIndex(Goods.class);
        // 配置映射
        this.template.putMapping(Goods.class);
        int page = 1;
        int rows = 100;
        int size = 0;
        do {
            // 查询分页数据
            PageResult<Spu> result = this.goodsClient.querySpuByPage(page, rows, true, null);
            List<Spu> spus = result.getItems();
            size = spus.size();
            if(size == 0){
                break;
            }
            // 创建Goods集合
            //List<Goods> goodsList = new ArrayList<>();
            // 遍历spu
            //for (Spu spu : spus) {
            //    Goods goods = this.searchService.buildGoods(spu);
            //    goodsList.add(goods);
            //}
            //使用Java8-流
            List<Goods> goodsList = spus.stream()
                    .map(searchService::buildGoods).collect(Collectors.toList());
            this.goodsRepository.saveAll(goodsList);
            page++;
        } while (size == 100);
    }
}
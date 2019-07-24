package com.shika.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.shika.common.enums.ExceptionEnum;
import com.shika.common.exceptions.SkException;
import com.shika.common.utils.JsonUtils;
import com.shika.common.viewObjects.PageResult;
import com.shika.item.pojo.*;
import com.shika.search.client.BrandClient;
import com.shika.search.client.CategoryClient;
import com.shika.search.client.GoodsClient;
import com.shika.search.client.SpecificationClient;
import com.shika.search.pojo.Goods;
import com.shika.search.pojo.SearchRequestBody;
import com.shika.search.pojo.SearchResult;
import com.shika.search.repository.GoodsRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SearchService {
    @Autowired
    private CategoryClient categoryClient;
    @Autowired
    private BrandClient brandClient;
    @Autowired
    private GoodsClient goodsClient;
    @Autowired
    private SpecificationClient specificationClient;
    @Autowired
    private GoodsRepository goodsRepository;
    @Autowired
    private ElasticsearchTemplate template;

    /**
     * 把SPU变为Goods，然后写入索引库
     *
     * @param spu
     * @return
     */
    public Goods buildGoods(Spu spu) {
        Goods goods = new Goods();
        // 查询商品分类名称
        List<Category> categorys = this.categoryClient.queryCategoryNameByIds(
                Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
        if (CollectionUtils.isEmpty(categorys)) {
            throw new SkException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }
        List<String> categoryNames = categorys.stream().map(Category::getName)
                .collect(Collectors.toList());
        //查询品牌
        Brand brand = brandClient.queryBrandNameById(spu.getBrandId());
        if (brand == null) {
            throw new SkException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        //搜索字段
        String all = spu.getTitle() + " " + StringUtils.join(categoryNames, " ")
                + brand.getName();

        // 查询sku
        List<Sku> skus = this.goodsClient.querySkuBySpuId(spu.getId());
        // 处理sku，仅封装id、价格、标题、图片，并获得价格集合
        List<Long> prices = new ArrayList<>();
        List<Map<String, Object>> skuList = new ArrayList<>();
        skus.forEach(sku -> {
            prices.add(sku.getPrice());
            Map<String, Object> skuMap = new HashMap<>();
            skuMap.put("id", sku.getId());
            skuMap.put("title", sku.getTitle());
            skuMap.put("price", sku.getPrice());
            skuMap.put("image", StringUtils.isBlank(sku.getImages()) ?
                    "" : StringUtils.split(sku.getImages(), ",")[0]);
            skuList.add(skuMap);
        });

        // 查询详情
        SpuDetail spuDetail = this.goodsClient.queryDetailById(spu.getId());
        // 查询规格参数
        List<SpecParam> params = this.specificationClient.queryParamList(null, spu.getCid3(), true);
        // 处理规格参数,通用+特有
        Map<String, String> genericSpecs = JsonUtils.parseMap(spuDetail.getGenericSpec(),
                String.class, String.class);
        Map<String, List<String>> specialSpecs = JsonUtils.nativeRead(spuDetail.getSpecialSpec(),
                new TypeReference<Map<String, List<String>>>() {
                });
        // 获取可搜索的规格参数
        Map<String, Object> searchSpec = new HashMap<>();

        // 过滤规格模板，把所有可搜索的信息保存到Map中
        Map<String, Object> specMap = new HashMap<>();
        params.forEach(p -> {
            if (p.getSearching()) {
                if (p.getGeneric()) {
                    String value = genericSpecs.get(p.getId().toString());
                    if (p.getNumeric()) {
                        value = chooseSegment(value, p); //按数值所在段存储
                    }
                    specMap.put(p.getName(), StringUtils.isBlank(value) ? "其它" : value);
                } else {
                    specMap.put(p.getName(), specialSpecs.get(p.getId().toString()));
                }
            }
        });
        goods.setId(spu.getId());
        goods.setSubTitle(spu.getSubTitle());
        goods.setBrandId(spu.getBrandId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setCreateTime(spu.getCreateTime());
        goods.setPrice(prices);
        goods.setAll(all);
        goods.setSkus(JsonUtils.serialize(skuList));
        goods.setSpecs(specMap);
        return goods;
    }

    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if (segs.length == 2) {
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if (val >= begin && val < end) {
                if (segs.length == 1) {
                    result = segs[0] + p.getUnit() + "以上";
                } else if (begin == 0) {
                    result = segs[1] + p.getUnit() + "以下";
                } else {
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    /**
     * ElasticSearch搜索
     *
     * @param requestBody
     * @return
     */
    /* 相当于完成以下ElasticSearch搜索语句
        GET /goods/_search
        {
            "query": {
            "match": {
                "all": "手机"
            }
        },
            "_source": ["id", "subTitle", "skus"]
        }
        */
    public PageResult<Goods> search(SearchRequestBody requestBody) {
        String key = requestBody.getKey();
        // 判断是否有搜索条件，如果没有，直接返回null。不允许搜索全部商品
        if (StringUtils.isBlank(key)) {
            return null;
        }
        int page = requestBody.getPage();
        int size = requestBody.getSize();
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //结果过滤
        queryBuilder.withSourceFilter(new FetchSourceFilter(
                new String[]{"id", "subTitle", "skus"}, null));
        //分页,elastic分页从第0页开始
        queryBuilder.withPageable(PageRequest.of(page - 1, size));
        //match过滤
        QueryBuilder basicQuery = buildBasicQueryWithFilter(requestBody);
        queryBuilder.withQuery(basicQuery);

        //聚合（按分类、品牌过滤搜索）
        String categoryAgg = "category_agg";
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAgg).field("cid3"));
        String brandAgg = "brand_agg";
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAgg).field("brandId"));
        //查询,聚合结果要用template
        AggregatedPage<Goods> result = template.queryForPage(queryBuilder.build(), Goods.class);
        //解析结果
        long totalElements = result.getTotalElements();
        long totalPages = result.getTotalPages();
        List<Goods> goodsList = result.getContent();
        //解析聚合结果
        Aggregations aggregations = result.getAggregations();
        List<Category> categories = parseCategoryAgg(aggregations.get(categoryAgg));
        List<Brand> brands = parseBrandAgg(aggregations.get(brandAgg));

        //规格参数聚合
        List<Map<String, Object>> specs = null;
        if (categories != null && categories.size() == 1) {
            //商品分类存在且数量为1，可以聚合规格参数
            specs = buildSpecificationAgg(categories.get(0).getId(), basicQuery);
        }
        return new SearchResult(totalElements, totalPages, goodsList, categories, brands, specs);
    }


    /**
     * 实现布尔查询
     *
     * @param requestBody
     * @return
     */
        /*
            GET /heima/_search
            {
                "query":{
                "bool":{
                    "must":{ "match": { "title": "小米手机",operator:"and"}},
                    "filter":{
                        "range":{"price":{"gt":2000.00,"lt":3800.00}}
                    }
                }
            }
            }
        */
    private QueryBuilder buildBasicQueryWithFilter(SearchRequestBody requestBody) {
        //创建布尔查询
        BoolQueryBuilder QueryBuilder = QueryBuilders.boolQuery();
        QueryBuilder.must(QueryBuilders.matchQuery("all", requestBody.getKey()));
        Map<String, String> map = requestBody.getFilter();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            if (!"cid3".equals(key) && !"brandId".equals(key)) {
                key = "specs." + key + ".keyword";
            }
            String value = entry.getValue();
            QueryBuilder.filter(QueryBuilders.termQuery(key, value));
        }
        return QueryBuilder;
    }

    private List<Map<String, Object>> buildSpecificationAgg(Long cid, QueryBuilder basicQuery) {
        List<Map<String, Object>> specs = new ArrayList<>();
        //查询要聚合的规格参数
        List<SpecParam> specParams = specificationClient.queryParamList(null, cid, Boolean.TRUE);
        //聚合
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withQuery(basicQuery); //在搜索结果基础上聚合
        for (SpecParam specParam : specParams) {
            String name = specParam.getName();
            queryBuilder.addAggregation(
                    AggregationBuilders.terms(name).field("specs." + name + ".keyword"));
        }
        //获取聚合结果
        AggregatedPage<Goods> result = template.queryForPage(queryBuilder.build(), Goods.class);
        Aggregations aggregations = result.getAggregations();
        for (SpecParam specParam : specParams) {
            //规格参数名
            String name = specParam.getName();
            StringTerms aggregation = aggregations.get(name);
            List<String> options = aggregation.getBuckets().stream()
                    .map(b -> b.getKeyAsString()).collect(Collectors.toList());
            //准备map
            Map<String, Object> map = new HashMap<>();
            map.put("k", name);
            map.put("options", options);
            specs.add(map);
        }
        return specs;
    }

    private List<Brand> parseBrandAgg(LongTerms terms) {
        List<Long> ids = terms.getBuckets().stream()
                .map(b -> b.getKeyAsNumber().longValue()).collect(Collectors.toList());
        List<Brand> brands = brandClient.queryBrandsByIds(ids);
        return brands;
    }

    private List<Category> parseCategoryAgg(LongTerms terms) {
        List<Long> ids = terms.getBuckets().stream()
                .map(b -> b.getKeyAsNumber().longValue()).collect(Collectors.toList());
        List<Category> categories = categoryClient.queryCategoryNameByIds(ids);
        return categories;
    }

    /**
     * 对搜索的索引库进行创建或修改
     *
     * @param spuId
     */
    public void createOrUpdateIndex(Long spuId) {
        Spu spu = goodsClient.querySpuById(spuId);
        if (spu == null) {
            log.error("索引对应的spu不存在，spuId:{}", spuId);
            //抛出异常，让消息回滚
            throw new RuntimeException();
        }
        Goods goods = buildGoods(spu);
        goodsRepository.save(goods);
    }

    /**
     * 对搜索的索引库进行删除
     *
     * @param spuId
     */
    public void deleteIndex(Long spuId) {
        goodsRepository.deleteById(spuId);
    }
}

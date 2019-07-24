package com.shika.page.service;

import com.shika.common.utils.ThreadUtils;
import com.shika.item.pojo.*;
import com.shika.page.client.BrandClient;
import com.shika.page.client.CategoryClient;
import com.shika.page.client.GoodsClient;
import com.shika.page.client.SpecificationClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PageService {
    @Autowired
    private BrandClient brandClient;
    @Autowired
    private CategoryClient categoryClient;
    @Autowired
    private GoodsClient goodsClient;
    @Autowired
    private SpecificationClient specificationClient;
    @Autowired
    private TemplateEngine templateEngine;

    public Map<String, Object> loadModel(Long spuId) {
        Map<String, Object> model = new HashMap<>();
        Spu spu = goodsClient.querySpuById(spuId);
        List<Sku> skus = spu.getSkus();
        SpuDetail detail = spu.getSpuDetail();
        Brand brand = brandClient.queryBrandNameById(spu.getBrandId());
        List<Category> categories = categoryClient.queryCategoryNameByIds(
                Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
        List<SpecGroup> specs = specificationClient.querySpecGroupByCid(spu.getCid3());
        model.put("title", spu.getTitle());
        model.put("subTitle", spu.getSubTitle());
        model.put("skus", skus);
        model.put("detail", detail);
        model.put("brand", brand);
        model.put("categories", categories);
        model.put("specs", specs);
        return model;
    }

    /**
     * 创建静态html页面
     * Sk-page微服务与nginx部署在一起，生成静态页保存至nginx目录
     * 现在只能手动将生成的静态页复制到nginx:/opt/nginx/html/item/目录下
     *
     * @param spuId
     * @throws Exception
     */
    public void createHtml(Long spuId) {
        PrintWriter writer = null;
        try {
            // 获取页面数据
            Map<String, Object> spuMap = loadModel(spuId);
            // 创建thymeleaf上下文对象
            Context context = new Context();
            // 把数据放入上下文对象
            context.setVariables(spuMap);
            // 创建输出流
            File file = new File("C:\\Users\\lenovo\\Desktop\\乐优商城\\乐优商城《项目笔记》\\day14笔记\\" + spuId + ".html");
            if (file.exists()) {
                file.delete();
            }
            writer = new PrintWriter(file, "UTF-8");
            // 执行页面静态化方法
            templateEngine.process("item", context, writer);
        } catch (Exception e) {
            log.error("页面静态化出错：{}，" + e, spuId);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * 新建线程处理页面静态化
     *
     * @param spuId
     */
    public void asyncExcute(Long spuId) {
        ThreadUtils.execute(() -> createHtml(spuId));
        /*ThreadUtils.execute(new Runnable() {
            @Override
            public void run() {
                createHtml(spuId);
            }
        });*/
    }

    public void deleteHtml(Long spuId) {
        File file = new File("C:\\Users\\lenovo\\Desktop\\乐优商城\\乐优商城《项目笔记》\\day14笔记\\" + spuId + ".html");
        if (file.exists()) {
            file.delete();
        }
    }
}

package com.shika.item.web;

import com.shika.common.viewObjects.PageResult;
import com.shika.item.pojo.Brand;
import com.shika.item.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created by Jiang on 2019/6/24.
 */
@RestController
@RequestMapping("brand")
public class BrandController {

    @Autowired
    private BrandService brandService;

    @GetMapping("page")
    public ResponseEntity<PageResult<Brand>> queryBrandByPage(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "rows", defaultValue = "5") Integer rows,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "desc", defaultValue = "false") Boolean desc,
            @RequestParam(value = "key", required = false) String key) {
        PageResult<Brand> result = this.brandService.queryBrandByPageAndSort(page,rows,sortBy,desc, key);
        //在service层用通用异常处理
//        if (result == null || result.getItems().size() == 0) {
//            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//        }
        return ResponseEntity.ok(result);
    }

    /**新增品牌
     * Void:无返回值
     * @param brand
     * @param cids
     * @return
     */
    //请求表单数据：
    //name: 庆云牌
    //image: http://image.shika.com/group1/M00/00/00/wKgXgV0UlpmAVITIAAG0CkQ0yvc796.jpg
    //cids: 733
    //letter: Q
    @PostMapping
    public ResponseEntity<Void> saveBrand(Brand brand, @RequestParam("cids") List<Long> cids){
        brandService.saveBrand(brand, cids);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}

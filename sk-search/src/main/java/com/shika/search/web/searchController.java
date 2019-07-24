package com.shika.search.web;

import com.shika.common.viewObjects.PageResult;
import com.shika.search.pojo.Goods;
import com.shika.search.pojo.SearchRequestBody;
import com.shika.search.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class searchController {
    @Autowired
    private SearchService searchService;

    /**
     * 搜索功能
     *
     * @param requestBody
     * @return
     */
    @PostMapping("page")
    public ResponseEntity<PageResult<Goods>> search(@RequestBody SearchRequestBody requestBody) {
        return ResponseEntity.ok(searchService.search(requestBody));

    }
}

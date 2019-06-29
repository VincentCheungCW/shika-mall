package com.shika.item.pojo;

import lombok.Data;
import tk.mybatis.mapper.annotation.KeySql;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by Jiang on 2019/6/24.
 */
@Data
@Table(name = "tb_brand")
public class Brand {
    @Id
    @KeySql(useGeneratedKeys = true)
    private Long id;
    private String name;// 品牌名称
    private String image;// 品牌图片url:"http://image.shika.com/group1/M00/00/00/wKgXgV0UlpmAVITIAAG0CkQ0yvc796.jpg"
    private Character letter;
}

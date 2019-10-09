package com.leyou.goods.service;

import com.leyou.goods.client.BrandClient;
import com.leyou.goods.client.CategoryClient;
import com.leyou.goods.client.GoodsClient;
import com.leyou.goods.client.SpecificationClient;
import com.leyou.item.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Service
public class GoodsService {

    @Autowired
    private BrandClient brandClient;
    @Autowired
    private CategoryClient categoryClient;
    @Autowired
    private GoodsClient goodsClient;
    @Autowired
    private SpecificationClient specificationClient;

    public Map<String, Object > loadData(Long id){
        HashMap<String, Object> model = new HashMap<>();
        //查询spu
        Spu spu = this.goodsClient.querySpuById(id);

        //查询spuDetail
        SpuDetail spuDetail = this.goodsClient.querySpuDetailById(id);

        //查询brand
        Brand brand = this.brandClient.queryBrandById(spu.getBrandId());

        //查询分类
        List<Long> ids = Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3());
        List<String> names = this.categoryClient.queryNameByIds(ids);
        List<Map<String, Object>> categories = new ArrayList<>(3);
        HashMap<String, Object> map = null;
        for(int i = 0 ; i < ids.size(); i ++){
            map = new HashMap<>();
            map.put("id",ids.get(0));
            map.put("name", names.get(0));
            categories.add(map);
        }
        
        //skus
        List<Sku> skus = this.goodsClient.querySkuBySpuId(id);

        //查询规格参数
        Specification specification = this.specificationClient.querySpecificationByCategoryId(spu.getCid3());

        //特殊规格参数
        HashMap<String, Object> paramMap = new HashMap<>();
        //specification.getSpecifications(), 获取global 为false的参数
        // 数据库与黑马不同需要自行修改
        // ...

        model.put("spu",null);
        model.put("spuDetail",spuDetail);
        model.put("categories",categories);
        model.put("brand",brand);
        model.put("skus",skus);
        model.put("groups",specification);
        model.put("paramMap",paramMap);
        return model;
    }
}

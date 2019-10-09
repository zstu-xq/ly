package com.leyou.search.service;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyou.item.pojo.*;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecificationClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.repository.GoodsRepository;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecificationClient specificationClient;

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private GoodsRepository goodsRepository;

    private ObjectMapper MAPPER = new ObjectMapper();

    public static final Logger logger = LoggerFactory.getLogger(SearchService.class);


    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    public Goods buildGoods(Spu spu) throws IOException {
        Goods goods = new Goods();

        // 查询商品分类名称
        List<String> names = this.categoryClient.queryNameByIds(
                Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));

        Brand brand = this.brandClient.queryBrandById(spu.getBrandId());

        // 查询详情
        SpuDetail spuDetail = this.goodsClient.querySpuDetailById(spu.getId());

        // 查询SKUS
        List<Sku> skus = this.goodsClient.querySkuBySpuId(spu.getId());
        //规格参数化

        //价格集合
        List<Long> prices = new ArrayList<>();
        //sku集合,不用所有字段，用Map有选择的存需要的字段
        List<Map<String, Object>> skuMapList = new ArrayList<>();

        skus.forEach(sku -> {
            prices.add(sku.getPrice());
            Map<String, Object> map =  new HashMap<>();
            map.put("id", sku.getId());
            map.put("title", sku.getTitle());
            map.put("price", sku.getPrice());
            map.put("image", StringUtils.isBlank(sku.getImages()) ? "" : StringUtils.split(sku.getImages(), ",")[0]);
            skuMapList.add(map);
        });
        // 处理规格参数
        //通用的规格参数反序列化
        List<Map<String, Object>> genericSpecs = MAPPER.readValue(spuDetail.getSpecifications(),
                new TypeReference<List<Map<String, Object>>>() {});
        //特用的规格参数反序列化
        Map<String, List<Object>> specialSpecs = MAPPER.readValue(spuDetail.getSpecTemplate(),
                new TypeReference<Map<String, Object>>() {});


        // 过滤规格模板，把所有可搜索的信息保存到Map中
        Map<String, Object> specMap = new HashMap<>();

        specialSpecs.forEach((k,v) -> {
            specMap.put(k,v);
        });

        genericSpecs.forEach(k -> {
            Map<String,Object> m = k;
            List<Map<String,String>> params = (List<Map<String, String>>) m.get("params");
            params.forEach( p -> {
                //判断是否可搜索，通用的
                if(StringUtils.equals("true",p.get("searchable"))&&
                        StringUtils.equals("true",p.get("global"))){
                    specMap.put(p.get("k"),p.get("v"));
                }
            });

        });
        goods.setId(spu.getId());
        goods.setBrandId(spu.getBrandId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setSubTitle(spu.getSubTitle());
        goods.setCreateTime(spu.getCreateTime());
        goods.setAll(spu.getTitle() + " " + StringUtils.join(names, " ") + " " + brand.getName());
        //所有sku价格
        goods.setPrice(prices);
        //spu下所有sku
        goods.setSkus(MAPPER.writeValueAsString(skuMapList));
        //获取所有查询的规格参数
        goods.setSpecs(specMap);
        return goods;
    }


    public SearchResult search(SearchRequest request) {
        String key = request.getKey();
        // 判断是否有搜索条件，如果没有，直接返回null。不允许搜索全部商品
        if (StringUtils.isBlank(key)) {
            return null;
        }

        // 构建查询条件
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();

        // 1、对key进行全文检索查询
       // QueryBuilder basicQuery = QueryBuilders.matchQuery("all", key).operator(Operator.AND);
        BoolQueryBuilder basicQuery = buildBoolQueryBuilder(request);
        queryBuilder.withQuery(basicQuery);

        // 2、通过sourceFilter设置返回的结果字段,我们只需要id、skus、subTitle
        queryBuilder.withSourceFilter(new FetchSourceFilter(
                new String[]{"id","skus","subTitle"}, null));

        //添加分类和品牌的聚合
        String categoryAggName = "categories";
        String brandAggName = "brands";
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));
        // 3、分页
        // 准备分页参数
        int page = request.getPage();
        int size = request.getSize();
        queryBuilder.withPageable(PageRequest.of(page - 1, size));

        // 4、查询，获取结果
        AggregatedPage<Goods> pageInfo = (AggregatedPage)this.goodsRepository.search(queryBuilder.build());
        List<Map<String, Object>> categories = getCategoryAggResult(pageInfo.getAggregation(categoryAggName));
        List<Brand> brands = getBrandAggResult(pageInfo.getAggregation(brandAggName));

        //判断是否规格参数聚合
        if (CollectionUtils.isEmpty(categories) && categories.size() == 1){
            List<Map<String, Object>> specs  = getSpecsAggResult((Long)categories.get(0).get("id"), basicQuery);
        }
        // 封装结果并返回
        return new SearchResult(pageInfo.getTotalElements(), (long) pageInfo.getTotalPages(), pageInfo.getContent(), categories, brands,null);
    }

    private BoolQueryBuilder buildBoolQueryBuilder(SearchRequest request) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //添加基本查询
        boolQueryBuilder.must(QueryBuilders.matchQuery("all", request.getKey()).operator(Operator.AND));
        //添加过滤条件
        //获取用用户选择的过滤信息
        Map<String, String> filter = request.getFilter();
        // 过滤条件构建器
        BoolQueryBuilder filterQueryBuilder = QueryBuilders.boolQuery();
        for (Map.Entry<String, String> entry : filter.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            // 商品分类和品牌要特殊处理
            if (key != "cid3" && key != "brandId") {
                key = "specs." + key + ".keyword";
            }
            // 字符串类型，进行term查询
            filterQueryBuilder.must(QueryBuilders.termQuery(key, value));
        }
        // 添加过滤条件
        boolQueryBuilder.filter(filterQueryBuilder);
        return boolQueryBuilder;

    }

    /**
     * 根据查询条件聚合规格参数
     * @param id
     * @param query
     * @return
     */
    private List<Map<String, Object>> getSpecsAggResult(Long id, QueryBuilder query) {
        try {
            // 不管是全局参数还是sku参数，只要是搜索参数，都根据分类id查询出来
            // 数据库与视频不同，代码进行部分修改
            Specification specification = this.specificationClient.querySpecificationByCategoryId(id);
            String s = specification.getSpecifications();
            List<Map<String,Object>> params =  MAPPER.readValue(s,
                    new TypeReference<List<Map<String, Object>>>() {});
            List<Map<String, Object>> specs = new ArrayList<>();

            NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
            queryBuilder.withQuery(query);

            // 聚合规格参数
            params.forEach(p -> {
                List<Map<String,String>> mapList = (List<Map<String, String>>) p.get("params");
                mapList.forEach( k -> {
                    if(StringUtils.equals(k.get("search"), "true")){
                        String key = k.get("k");
                        queryBuilder.addAggregation(AggregationBuilders.terms(key).field("specs." + key + ".keyword"));
                    }
                });
            });

            // 查询
            Map<String, Aggregation> aggs = this.elasticsearchTemplate.query(queryBuilder.build(),
                    SearchResponse::getAggregations).asMap();

            // 解析聚合结果
            params.forEach(param -> {
                List<Map<String,String>> mapList = (List<Map<String, String>>) param.get("params");
                mapList.forEach( k -> {
                    if(StringUtils.equals(k.get("search"), "true")){
                        String key = k.get("k");
                        Map<String, Object> spec = new HashMap<>();
                        spec.put("k", key);
                        StringTerms terms = (StringTerms) aggs.get(key);
                        spec.put("options", terms.getBuckets().stream().map(StringTerms.Bucket::getKeyAsString));
                        specs.add(spec);
                    }
                });
            });

            return specs;
        } catch (Exception e) {
            logger.error("规格聚合出现异常：", e);
            return null;
        }

    }

    /**
     * 解析品牌的聚合结果集
     * @param aggregation
     * @return
     */
    private List<Brand> getBrandAggResult(Aggregation aggregation) {
        LongTerms terms = (LongTerms) aggregation;

        return  terms.getBuckets().stream().map(bucket -> {
            return this.brandClient.queryBrandById(bucket.getKeyAsNumber().longValue());
        }).collect(Collectors.toList());
    }

    /**
     * 解析分类的聚合结果集
     * @param aggregation
     * @return
     */
    private List<Map<String, Object>> getCategoryAggResult(Aggregation aggregation) {
        LongTerms terms = (LongTerms) aggregation;
        return terms.getBuckets().stream().map(bucket -> {
            Map<String, Object> map = new HashMap<String, Object>();
            Long id = bucket.getKeyAsNumber().longValue();
            List<String> names = this.categoryClient.queryNameByIds(Arrays.asList(id));
            map.put("id", id);
            map.put("name", names.get(0));
            return map;
        }).collect(Collectors.toList());
    }

    public void save(Long id) throws IOException {
        Spu spu = this.goodsClient.querySpuById(id);
        Goods goods = buildGoods(spu);
        this.goodsRepository.save(goods);
    }

    public void delete(Long id) {
        this.goodsRepository.deleteById(id);
    }
}

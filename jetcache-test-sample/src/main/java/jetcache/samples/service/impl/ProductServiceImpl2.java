package jetcache.samples.service.impl;

import com.alibaba.fastjson.JSON;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.jetcahe.support.Pair;
import com.jetcahe.support.annotation.ListCacheInvalidate;
import com.jetcahe.support.annotation.ListCached;
import com.jetcahe.support.extend.JedisPileLineOperator;
import jetcache.samples.dto.request.ProductRequest;
import jetcache.samples.dto.response.ProductResponse;
import jetcache.samples.dto.response.SkuResponse;
import jetcache.samples.service.ProductService;
import jetcache.samples.service.SkuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl2 implements ProductService {
    private static Logger logger = LoggerFactory.getLogger(ProductServiceImpl2.class);

    @Autowired
    private SkuService skuService;


    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Override
    public List<ProductResponse> listProduct(ProductRequest request) {
        List<String> productCodes = request.getProductCodes();
        List<ProductResponse> productResponses = new ArrayList<>(productCodes.size());
        for(String productCode:productCodes){
            productResponses.add(getByProductCode(productCode));
        }
        return productResponses;
    }

    /**
     * 采用原生api缓存
     * @param productCode
     * @return
     */
    @Override
    public ProductResponse getByProductCode(String productCode) {
        String pJson=null;
        try {
             pJson = redisTemplate.opsForValue().get("product:"+productCode);
        }catch (Exception e){
            logger.error("读缓存失败:",e);
        }
        if(pJson != null){
            ProductResponse productResponse= JSON.parseObject(pJson,ProductResponse.class);
            return productResponse;
        }
        ProductResponse productResponse = getProductFromDb(productCode);
        List<SkuResponse> skuResponses = skuService.listByProductCode(productCode);
        productResponse.setSkuResponses(skuResponses);
        try {
            redisTemplate.opsForValue().set("product:"+productCode,JSON.toJSONString(productResponse,100));
        }catch (Exception e){
            logger.error("写缓存失败:",e);
        }
        return productResponse;
    }

    /**
     * 基于jetcache 集群支持改造
     * @param productCode
     * @return
     */
    @Cached(name = "product:",key = "#productCode",expire = 100,cacheType = CacheType.BOTH,localExpire = 100)
    @Override
    public ProductResponse getByProductCode2(String productCode) {
        ProductResponse response = new ProductResponse();
        response.setProductCode(productCode);
        List<SkuResponse> skuResponses = skuService.listByProductCode(productCode);
        response.setSkuResponses(skuResponses);
        return response;
    }

    @Cached(name = "product3:",key = "#productCode",expire = 100)
    @Override
    public ProductResponse getByProductCode3(String productCode) {
        ProductResponse response = new ProductResponse();
        response.setProductCode(productCode);
        List<SkuResponse> skuResponses = skuService.listByProductCode(productCode);
        response.setSkuResponses(skuResponses);
        return response;
    }

    private ProductResponse getProductFromDb(String productCode) {
        ProductResponse productResponse = new ProductResponse();
        productResponse.setProductCode(productCode);
        return productResponse;
    }

    private List<ProductResponse> getListProductFromDb(List<String> productCodes) {
        List<ProductResponse> productResponses = new ArrayList<>(productCodes.size());
        for(String productCode:productCodes){
            ProductResponse productResponse = new ProductResponse();
            productResponse.setProductCode(productCode);
            productResponses.add(productResponse);
        }
        return productResponses;
    }



    private String productCachePrefix="product:";

    /**
     * 原生管道批是查询
     * @param request
     * @return
     */
    @Override
    public List<ProductResponse> listProduct2(ProductRequest request) {
        List<String> productCodes = request.getProductCodes();
        Map<String/*cacheKey*/, String> keyProductCodeMap = productCodes.stream().map(p -> new Pair<String,String>(productCachePrefix + p, p)).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
        Set<String> cacheKeys = keyProductCodeMap.keySet();
        //1.查缓存
        List<Pair<String/*cacheKey*/, ProductResponse>> productCachePair= null;
        try {
            productCachePair  = JedisPileLineOperator.batchReadPair(cacheKeys, ProductResponse.class);
        }catch (Exception e){
            logger.error("读缓存失败:",e);
        }
        List<ProductResponse> cacheProducts = new ArrayList<>();
        //无缓存值的productCode
        List<String> noCacheProductCodes = new ArrayList<>();
        if(productCachePair != null){
            productCachePair.forEach(p->{
                if(p.getValue() != null){
                    cacheProducts.add(p.getValue());
                }else {
                    String productCode = keyProductCodeMap.get(p.getKey());
                    noCacheProductCodes.add(productCode);
                }
            });
        }
        if(noCacheProductCodes.isEmpty()){
            return cacheProducts;
        }

        //2.从库里查询数据
        List<ProductResponse> listProductFromDb = getListProductFromDb(noCacheProductCodes);
        //批量查询product 对应sku 信息，并合并
        List<SkuResponse> skuResponses = skuService.listByProductCodes(request.getProductCodes());
        Map<String, List<SkuResponse>> productSkuMap = skuResponses.stream().collect(Collectors.groupingBy(SkuResponse::getProductCode));
        listProductFromDb.forEach((p)->p.setSkuResponses(productSkuMap.get(p.getProductCode())));

        //3 写缓存
        List<Pair<String,ProductResponse>> toCacheList = listProductFromDb.stream().map(p -> new Pair<String,ProductResponse>(productCachePrefix + p.getProductCode(), p)).collect(Collectors.toList());
        try {
            JedisPileLineOperator.batchWritePair(toCacheList,100);
        }catch (Exception e){
            logger.error("写缓存失败:",e);
        }

        //4.合并缓存及db数据
        cacheProducts.addAll(listProductFromDb);

        return cacheProducts;
    }

    /**
     * 基于jetcahe 改造批量查询
     * #request.productCodes
     * #request.productCodes[0],product:xx1
     * #request.productCodes[1],product:xx2
     * #request.productCodes[2],product:xx3
     * #request.productCodes[3],product:xx4
     * #request.productCodes[4],product:xx5
     * @param request
     * @return
     */
    @ListCached(name = "product:",key = "#request.productCodes[",returnKey = "#[.productCode",expire = 100)
    @Override
    public List<ProductResponse> listProduct3(ProductRequest request) {
        List<ProductResponse> listProductFromDb = getListProductFromDb(request.getProductCodes());
        List<SkuResponse> skuResponses = skuService.listByProductCodes(request.getProductCodes());
        Map<String, List<SkuResponse>> productSkuMap = skuResponses.stream().collect(Collectors.groupingBy(SkuResponse::getProductCode));
        listProductFromDb.forEach((p)->p.setSkuResponses(productSkuMap.get(p.getProductCode())));
        return listProductFromDb;
    }
    @ListCached(name = "product:",key = "#productCodes[",returnKey = "#[.productCode",expire = 100,cacheType = CacheType.BOTH,localExpire = 1999)
    @Override
    public List<ProductResponse> listProduct4(List<String> productCodes) {
        List<ProductResponse> listProductFromDb = getListProductFromDb(productCodes);
        List<SkuResponse> skuResponses = skuService.listByProductCodes(productCodes);
        Map<String, List<SkuResponse>> productSkuMap = skuResponses.stream().collect(Collectors.groupingBy(SkuResponse::getProductCode));
        listProductFromDb.forEach((p)->p.setSkuResponses(productSkuMap.get(p.getProductCode())));
        return listProductFromDb;
    }

    @ListCacheInvalidate(name = "product:",key = "#request.productCodes[")
    @Override
    public void batchDelete(ProductRequest request) {

    }
}

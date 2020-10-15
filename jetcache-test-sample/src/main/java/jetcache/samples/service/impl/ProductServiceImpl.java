package jetcache.samples.service.impl;

import com.alibaba.fastjson.JSON;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.jetcahe.support.Pair;
import com.jetcahe.support.annotation.ListCacheInvalidate;
import com.jetcahe.support.annotation.ListCached;
import com.jetcahe.support.extend.JedisPileLineOperator;
import jetcache.samples.dao.ProductMapper;
import jetcache.samples.dto.request.ProductRequest;
import jetcache.samples.dto.response.ProductResponse;
import jetcache.samples.dto.response.SkuPriceResponse;
import jetcache.samples.dto.response.SkuResponse;
import jetcache.samples.dto.response.SkuStockResponse;
import jetcache.samples.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private static Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Autowired
    private SkuService skuService;

    @Autowired
    private ProductImageService productImageService;

    @Autowired
    private SkuPriceService skuPriceService;

    @Autowired
    private SkuStockService skuStockService;



    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Autowired
    private ProductMapper productMapper;

    private String imageHost="http://image.xxx.com";



    /**
     * 根据商品编码获取商品详细信息
     * 1.获取商品主要信息
     * 2.获取商品图片
     * 3.获取商品sku信息
     * 4.获取sku 库存
     * 5.获取sku价格
     * @param productCode
     * @return
     */
    @Override
    public ProductResponse getByProductCode(String productCode) {
        //1.获取商品主要信息
        ProductResponse productResponse = productMapper.getProductByCode(productCode);
        //2.获取商品的图片
        List<String> productImages = productImageService.listByProductCode(productCode);
        //3.拼接图片host
        List<String> imagesWithHost = new ArrayList<>(productImages.size());
        for(String imageUrl:productImages){
            imagesWithHost.add(imageHost+imageUrl);
        }
        productResponse.setImages(imagesWithHost);

        //4获取商品sku信息
        List<SkuResponse> skuResponses = skuService.listByProductCode(productCode);
        //5获取商品sku对应的库存
        List<SkuStockResponse> skuStockResponseList = skuStockService.listByProductCode(productCode);
        //6 据skuCode 匹配对应的stock
        Map<String, SkuStockResponse> skuStockMap = skuStockResponseList.stream().collect(Collectors.toMap(SkuStockResponse::getSkuCode, Function.identity()));
        for(SkuResponse sku:skuResponses){
            SkuStockResponse skuStockResponse = skuStockMap.get(sku.getSkuCode());
            sku.setSkuStockResponse(skuStockResponse);
        }

        //7获取商品sku对应的价格
        List<SkuPriceResponse> skuPriceResponses = skuPriceService.listByProductCode(productCode);
        //8据skuCode 匹配对应的price
        Map<String, SkuPriceResponse> skuPriceMap = skuPriceResponses.stream().collect(Collectors.toMap(SkuPriceResponse::getSkuCode, Function.identity()));
        for(SkuResponse sku:skuResponses){
            SkuPriceResponse skuStockResponse = skuPriceMap.get(sku.getSkuCode());
            sku.setSkuPriceResponse(skuStockResponse);
        }
        productResponse.setSkuResponses(skuResponses);
        return productResponse;
    }

    public ProductResponse getByProductCode5(String productCode) {
        //1.获取商品主要信息
        ProductResponse productResponse = productMapper.getProductByCode(productCode);
        //2.获取商品的图片
        List<String> productImages = productImageService.listByProductCode(productCode);
        //3.拼接图片host
        List<String> imagesWithHost = buildImageUrlWithHost(productImages);
        productResponse.setImages(imagesWithHost);

        //4获取商品sku信息
        List<SkuResponse> skuResponses = skuService.listByProductCode(productCode);
        //5获取商品sku对应的库存
        List<SkuStockResponse> skuStockResponseList = skuStockService.listByProductCode(productCode);
        //6 据skuCode 匹配对应的stock
        mappingAndFillSkuStocks(skuResponses,skuStockResponseList);

        //7获取商品sku对应的价格
        List<SkuPriceResponse> skuPriceResponses = skuPriceService.listByProductCode(productCode);
        //8据skuCode 匹配对应的price
        mappingAndFillSkuPrice(skuResponses,skuPriceResponses);
        productResponse.setSkuResponses(skuResponses);
        return productResponse;
    }

    public List<String> listImageByProductCode(String productCode){
        //2.获取商品的图片
        List<String> productImages = productImageService.listByProductCode(productCode);
        //3.拼接图片host
        List<String> imagesWithHost = buildImageUrlWithHost(productImages);
        return imagesWithHost;
    }


    public ProductResponse getByProductCode6(String productCode) {
        //1.获取商品主要信息
        ProductResponse productResponse = productMapper.getProductByCode(productCode);
        //2.获取商品的图片
        List<String> productImages = productImageService.listByProductCode(productCode);
        productResponse.setImages(productImages);
        //4获取商品sku信息
        List<SkuResponse> skuResponses = skuService.listByProductCode(productCode);
        productResponse.setSkuResponses(skuResponses);
        return productResponse;
    }

    private void mappingAndFillSkuPrice(List<SkuResponse> skuResponses, List<SkuPriceResponse> skuPriceResponses) {
        Map<String, SkuPriceResponse> skuPriceMap = skuPriceResponses.stream().collect(Collectors.toMap(SkuPriceResponse::getSkuCode, Function.identity()));
        for(SkuResponse sku:skuResponses){
            SkuPriceResponse skuStockResponse = skuPriceMap.get(sku.getSkuCode());
            sku.setSkuPriceResponse(skuStockResponse);
        }
    }

    private void mappingAndFillSkuStocks(List<SkuResponse> skuResponses, List<SkuStockResponse> skuStockResponseList) {
        Map<String, SkuStockResponse> skuStockMap = skuStockResponseList.stream().collect(Collectors.toMap(SkuStockResponse::getSkuCode, Function.identity()));
        for(SkuResponse sku:skuResponses){
            SkuStockResponse skuStockResponse = skuStockMap.get(sku.getSkuCode());
            sku.setSkuStockResponse(skuStockResponse);
        }
    }

    private List<String> buildImageUrlWithHost( List<String> productImages ){
        List<String> imagesWithHost = new ArrayList<>(productImages.size());
        for(String imageUrl:productImages){
            imagesWithHost.add(imageHost+imageUrl);
        }
        return imagesWithHost;
    }




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

package jetcache.samples.service.impl;

import com.alibaba.fastjson.JSON;
import jetcache.samples.dto.WeworkResponse;
import jetcache.samples.service.ProductImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;

//@Service("imageCacheProxyService")
public class ProductImageCacheProxyService implements ProductImageService {


    private ProductImageService productImageService;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    public ProductImageCacheProxyService(ProductImageService imageService){
        this.productImageService = imageService;
    }

    @Override
    public List<String> listByProductCode(String productCode) {
        String imagesJsonStr=null;
        try {
            imagesJsonStr= redisTemplate.opsForValue().get("product-image" + productCode);
        }catch (Exception ex){
            //todo
        }
        if(!StringUtils.isEmpty(imagesJsonStr)){
            return JSON.parseArray(imagesJsonStr,String.class);
        }
        List<String> imageList = productImageService.listByProductCode(productCode);
        try {
            redisTemplate.opsForValue().set("product-image" + productCode,JSON.toJSONString(imageList));
        }catch (Exception ex){
            //todo
        }
        return imageList;
    }

    @Override
    public List<String> listByProductCode2(String productCode) {
        return null;
    }

    @Override
    public WeworkResponse requestWework() {
        return null;
    }
}

package jetcache.samples.service.impl;

import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import jetcache.samples.annotation.AsynTask;
import jetcache.samples.service.ProductImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("imageService")
public class ProductImageServiceImpl implements ProductImageService {

    private String imageHost="http://image.xxx.com";


//    @Cached(name = "product-image",key = "#productCode",expire = 100,cacheType = CacheType.REMOTE,localExpire = 100)
    @AsynTask(parentName = "getProductByCode",name = "listImage",subTasks = 0)
    @Override
    public List<String> listByProductCode(String productCode) {
        List<String> imageUrls = getImagesFromRemote(productCode);
        List<String> imageLIst = buildImageUrlWithHost(imageUrls);
        return imageLIst;
    }



    private List<String> getImagesFromDb(String productCode){
        return new ArrayList<>();
    }

    private List<String> getImagesFromRemote(String productCode){
        List<String> objects = new ArrayList<>();
        objects.add("xxxxx.jpg");
        objects.add("yyyyy.jpg");
        return objects;
    }

    private List<String> buildImageUrlWithHost( List<String> productImages ){
        List<String> imagesWithHost = new ArrayList<>(productImages.size());
        for(String imageUrl:productImages){
            imagesWithHost.add(imageHost+imageUrl);
        }
        return imagesWithHost;
    }


}

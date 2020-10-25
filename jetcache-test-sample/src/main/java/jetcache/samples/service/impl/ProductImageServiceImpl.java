package jetcache.samples.service.impl;

import jetcache.samples.annotation.MultiTask;
import jetcache.samples.dto.WeworkResponse;
import jetcache.samples.service.ImageRemoteService;
import jetcache.samples.service.ProductImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service("imageService")
public class ProductImageServiceImpl implements ProductImageService {

    private String imageHost="http://image.xxx.com";

    @Autowired(required = false)
    private ImageRemoteService imageRemoteService;

    @Autowired()
    private RestTemplate restTemplate;

    @Override
    public WeworkResponse requestWework() {
        //通过http client 调用企业微信服务
        String url = "http://xxx.com/xxxx/sss";
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.set("content-type", "application/json");
        HttpEntity requestEntity = new HttpEntity<>(null, headers);
        ResponseEntity<WeworkResponse> exchange = restTemplate.exchange(url, HttpMethod.POST, requestEntity, WeworkResponse.class);
        if (!exchange.getStatusCode().equals(HttpStatus.OK)) {
            assert false;
        }
        return exchange.getBody();
    }

    @Override
    public List<String> listByProductCode2(String productCode) {
        //从远程服务
        List<String> imageUrls = imageRemoteService.listByProductCode(productCode);
        List<String> imageLIst = buildImageUrlWithHost(imageUrls);
        return imageLIst;
    }



//    @Cached(name = "product-image",key = "#productCode",expire = 100,cacheType = CacheType.REMOTE,localExpire = 100)
    @MultiTask(parentName = "getProductByCode",name = "listImage")
    @Override
    public List<String> listByProductCode(String productCode) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

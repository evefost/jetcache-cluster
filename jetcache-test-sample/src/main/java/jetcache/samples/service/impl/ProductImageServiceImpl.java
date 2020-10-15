package jetcache.samples.service.impl;

import jetcache.samples.service.ProductImageService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductImageServiceImpl implements ProductImageService {

    private String imageHost="http://image.xxx.com";

    @Override
    public List<String> listByProductCode(String productCode) {
        List<String> imageUrls = getImagesFromDb(productCode);
        return buildImageUrlWithHost(imageUrls);
    }

    private List<String> getImagesFromDb(String productCode){
        return new ArrayList<>();
    }

    private List<String> buildImageUrlWithHost( List<String> productImages ){
        List<String> imagesWithHost = new ArrayList<>(productImages.size());
        for(String imageUrl:productImages){
            imagesWithHost.add(imageHost+imageUrl);
        }
        return imagesWithHost;
    }


}

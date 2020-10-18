package test.jetcache.samples;

import jetcache.samples.dto.response.ProductResponse;
import jetcache.samples.dto.response.SkuResponse;

import java.util.ArrayList;
import java.util.List;


public class ProxyReference implements ProxyReferenceInterface{



    public ProductResponse getProduct(){
        ProductResponse productResponse = new ProductResponse();
        productResponse.setProductCode("abc");
        SkuResponse sku = getSku();
        String image = getImage();
//        productResponse.setSkuResponses(sku);
        productResponse.setImage(image);
        return productResponse;
    }

    public SkuResponse getSku(){
        SkuResponse skuResponse = new SkuResponse();
        skuResponse.setSkuCode("1234");

        return skuResponse;
    }

    @Override
    public List<SkuResponse> listSku() {
        SkuResponse skuResponse = new SkuResponse();
        skuResponse.setSkuCode("1234");
        SkuResponse skuResponse2 = new SkuResponse();
        skuResponse2.setSkuCode("456");
        List<SkuResponse> objects = new ArrayList<>();
        objects.add(skuResponse);
        objects.add(skuResponse2);
        return objects;
    }

    public String getImage(){
        return "aaaa";
    }
}

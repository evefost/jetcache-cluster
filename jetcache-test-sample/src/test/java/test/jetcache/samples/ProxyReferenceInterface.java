package test.jetcache.samples;

import jetcache.samples.dto.response.ProductResponse;
import jetcache.samples.dto.response.SkuResponse;

import java.util.List;


public interface ProxyReferenceInterface {


    ProductResponse getProduct();

    SkuResponse getSku();

    List<SkuResponse> listSku();

    String getImage();
}

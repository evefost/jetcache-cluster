package jetcache.samples.service;

import jetcache.samples.dto.request.ProductRequest;
import jetcache.samples.dto.response.ProductResponse;

import java.util.List;

public interface ProductService {

    ProductResponse getByProductCode(String productCode);

    ProductResponse getByProductCode2(String productCode);

    List<ProductResponse> listProduct(ProductRequest request);

    List<ProductResponse> listProduct2(ProductRequest request);


    List<ProductResponse> listProduct3(ProductRequest request);

    List<ProductResponse> listProduct4(List<String> productCodes);

    void  batchDelete(ProductRequest request);
}

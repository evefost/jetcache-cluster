package jetcache.samples.dao;

import jetcache.samples.dto.response.ProductResponse;

public interface ProductMapper {

    ProductResponse getProductByCode(String productCode);
}

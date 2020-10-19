package jetcache.samples.dao;

import jetcache.samples.dto.response.SkuStockResponse;

import java.util.List;

public interface SkuStockMapper {

    List<SkuStockResponse> listByProductCode(String productCode);
}

package jetcache.samples.service;

import jetcache.samples.dto.response.SkuStockResponse;

import java.util.List;

public interface SkuStockService {

    List<SkuStockResponse> listByProductCode(String productCode);
}

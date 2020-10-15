package jetcache.samples.service;

import jetcache.samples.dto.response.SkuPriceResponse;

import java.util.List;

public interface SkuPriceService {

    List<SkuPriceResponse> listByProductCode(String productCode);
}

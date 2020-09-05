package jetcache.samples.service;

import jetcache.samples.dto.response.SkuResponse;

import java.util.List;

public interface SkuService {
    List<SkuResponse> listByProductCode(String productCode);

    List<SkuResponse> listByProductCodes(List<String> productCodes);
}

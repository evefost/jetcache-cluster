package jetcache.samples.dao;

import jetcache.samples.dto.response.SkuPriceResponse;

import java.util.List;

public interface SkuPriceMapper {

    List<SkuPriceResponse> listByProductCode(String productCode);
}

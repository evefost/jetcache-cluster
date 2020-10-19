package jetcache.samples.dao;

import jetcache.samples.dto.response.SkuResponse;

import java.util.List;

public interface SkuMapper {

    List<SkuResponse> listByProductCode(String productCode);
}

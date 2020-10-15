package jetcache.samples.service.impl;

import jetcache.samples.dto.response.SkuPriceResponse;
import jetcache.samples.service.SkuPriceService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SkuPriceServiceImpl implements SkuPriceService {


    @Override
    public List<SkuPriceResponse> listByProductCode(String productCode) {
        return null;
    }
}

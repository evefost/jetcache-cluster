package jetcache.samples.service.impl;

import jetcache.samples.dto.response.SkuPriceResponse;
import jetcache.samples.dto.response.SkuResponse;
import jetcache.samples.service.SkuPriceService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SkuPriceServiceImpl implements SkuPriceService {



    @Override
    public List<SkuPriceResponse> listByProductCode(String productCode) {
        return mockProductSkuPrice(productCode);
    }
    private AtomicInteger count = new AtomicInteger(1);
    Random random = new Random();
    private List<SkuPriceResponse> mockProductSkuPrice(String productCode) {
        int count = 10;
        List<SkuPriceResponse> responses = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            SkuPriceResponse skuResponse = new SkuPriceResponse();
            skuResponse.setProductCode(productCode);
            skuResponse.setSkuCode(generateCode());
            skuResponse.setPrice(i);
            responses.add(skuResponse);
        }
        return responses;
    }

    private String generateCode() {
        String code = "sku" + count.incrementAndGet();
        return code;
    }
}

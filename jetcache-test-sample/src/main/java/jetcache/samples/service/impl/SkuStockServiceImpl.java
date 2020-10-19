package jetcache.samples.service.impl;

import jetcache.samples.dto.response.SkuPriceResponse;
import jetcache.samples.dto.response.SkuStockResponse;
import jetcache.samples.service.SkuStockService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SkuStockServiceImpl implements SkuStockService {


    @Override
    public List<SkuStockResponse> listByProductCode(String productCode) {
        return mockProductSkuStock(productCode);
    }

    private AtomicInteger count = new AtomicInteger(1);
    Random random = new Random();
    private List<SkuStockResponse> mockProductSkuStock(String productCode) {
        int count = 10;
        List<SkuStockResponse> responses = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            SkuStockResponse skuResponse = new SkuStockResponse();
            skuResponse.setProductCode(productCode);
            skuResponse.setSkuCode(generateCode());
            skuResponse.setStock(i);
            responses.add(skuResponse);
        }
        return responses;
    }

    private String generateCode() {
        String code = "sku" + count.incrementAndGet();
        return code;
    }
}

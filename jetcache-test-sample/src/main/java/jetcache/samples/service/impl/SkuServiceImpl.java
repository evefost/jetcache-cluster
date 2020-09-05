package jetcache.samples.service.impl;

import jetcache.samples.dto.response.SkuResponse;
import jetcache.samples.service.SkuService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SkuServiceImpl implements SkuService {


    private AtomicInteger count = new AtomicInteger(1);

    Random random = new Random();

    @Override
    public List<SkuResponse> listByProductCode(String productCode) {
        return mockProductSku(productCode);
    }


    private String generateCode() {
        String code = "sku" + count.incrementAndGet();
        return code;
    }

    @Override
    public List<SkuResponse> listByProductCodes(List<String> productCodes) {
        List<SkuResponse> responses = new ArrayList<>();
        for (String productCode : productCodes) {
            List<SkuResponse> skus = mockProductSku(productCode);
            responses.addAll(skus);
        }
        return responses;
    }

    private List<SkuResponse> mockProductSku(String productCode) {
        int count = random.nextInt(10) + 1;
        List<SkuResponse> responses = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            SkuResponse skuResponse = new SkuResponse();
            skuResponse.setProductCode(productCode);
            skuResponse.setSkuCode(generateCode());
            responses.add(skuResponse);
        }
        return responses;
    }


}

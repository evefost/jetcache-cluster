package jetcache.samples.service.impl;

import jetcache.samples.annotation.AsynTask;
import jetcache.samples.dto.response.SkuPriceResponse;
import jetcache.samples.dto.response.SkuResponse;
import jetcache.samples.dto.response.SkuStockResponse;
import jetcache.samples.service.SkuPriceService;
import jetcache.samples.service.SkuService;
import jetcache.samples.service.SkuStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SkuServiceImpl implements SkuService {


    private AtomicInteger count = new AtomicInteger(1);

    Random random = new Random();

    @Autowired
    private SkuPriceService skuPriceService;

    @Autowired
    private SkuStockService skuStockService;

    @AsynTask(parentName = "getProductByCode",name = "listSku",subTasks = 0)
    @Override
    public List<SkuResponse> listByProductCode(String productCode) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        List<SkuResponse> skuResponses = mockProductSku(productCode);
        //5获取商品sku对应的库存
        List<SkuStockResponse> skuStockResponseList = skuStockService.listByProductCode(productCode);
        //6 据skuCode 匹配对应的stock
        mappingAndFillSkuStocks(skuResponses,skuStockResponseList);
        //7获取商品sku对应的价格
        List<SkuPriceResponse> skuPriceResponses = skuPriceService.listByProductCode(productCode);
        //8据skuCode 匹配对应的price
        mappingAndFillSkuPrice(skuResponses,skuPriceResponses);
        return skuResponses;
    }

    private void mappingAndFillSkuStocks(List<SkuResponse> skuResponses, List<SkuStockResponse> skuStockResponseList) {
        Map<String, SkuStockResponse> skuStockMap = skuStockResponseList.stream().collect(Collectors.toMap(SkuStockResponse::getSkuCode, Function.identity()));
        for(SkuResponse sku:skuResponses){
            SkuStockResponse skuStockResponse = skuStockMap.get(sku.getSkuCode());
            sku.setSkuStockResponse(skuStockResponse);
        }
    }

    private void mappingAndFillSkuPrice(List<SkuResponse> skuResponses, List<SkuPriceResponse> skuPriceResponses) {
        Map<String, SkuPriceResponse> skuPriceMap = skuPriceResponses.stream().collect(Collectors.toMap(SkuPriceResponse::getSkuCode, Function.identity()));
        for(SkuResponse sku:skuResponses){
            SkuPriceResponse skuStockResponse = skuPriceMap.get(sku.getSkuCode());
            sku.setSkuPriceResponse(skuStockResponse);
        }
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

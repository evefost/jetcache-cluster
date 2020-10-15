package jetcache.samples.dto.response;

public class SkuResponse {

    private String productCode;

    private String skuCode;

    private SkuStockResponse skuStockResponse;

    private SkuPriceResponse skuPriceResponse;

    public SkuPriceResponse getSkuPriceResponse() {
        return skuPriceResponse;
    }

    public void setSkuPriceResponse(SkuPriceResponse skuPriceResponse) {
        this.skuPriceResponse = skuPriceResponse;
    }

    public SkuStockResponse getSkuStockResponse() {
        return skuStockResponse;
    }

    public void setSkuStockResponse(SkuStockResponse skuStockResponse) {
        this.skuStockResponse = skuStockResponse;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }
}

package jetcache.samples.dto.response;

public class SkuResponse {

    private String productCode;

    private String skuCode;

    private SkuStockResponse skuStockResponse;

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

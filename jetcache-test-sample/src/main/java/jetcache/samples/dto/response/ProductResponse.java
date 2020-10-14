package jetcache.samples.dto.response;

import java.io.Serializable;
import java.util.List;

public class ProductResponse implements Serializable {

    private String productCode;


    private List<SkuResponse> skuResponses;

    private List<String> images;

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public List<SkuResponse> getSkuResponses() {
        return skuResponses;
    }

    public void setSkuResponses(List<SkuResponse> skuResponses) {
        this.skuResponses = skuResponses;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }
}

package jetcache.samples.dto.response;

import java.io.Serializable;
import java.util.List;

public class ProductResponse implements Serializable {

    private String productCode;

    private String image;


    private List<SkuResponse> skuResponses;

    private List<String> images;

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

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

package jetcache.samples.service.impl;

import jetcache.samples.service.ProductImageService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductImageServiceImpl implements ProductImageService {

    @Override
    public List<String> listByProductCode(String productCode) {
        return null;
    }

}

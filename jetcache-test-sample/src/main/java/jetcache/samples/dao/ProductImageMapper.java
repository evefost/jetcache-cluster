package jetcache.samples.dao;

import java.util.List;

public interface ProductImageMapper {

    List<String> listByProductCode(String productCode);
}

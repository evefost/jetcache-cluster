package jetcache.samples.service;

import java.util.List;
//模据一个远程rpc 服务接口
public interface ImageRemoteService {
    //获取图片url
    List<String> listByProductCode(String productCode);
}

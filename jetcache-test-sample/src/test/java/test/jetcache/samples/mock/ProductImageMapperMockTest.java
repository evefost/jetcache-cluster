package test.jetcache.samples.mock;

import jetcache.samples.dao.ProductImageMapper;
import org.junit.Before;
import org.junit.Test;
import test.jetcache.samples.BaseServiceTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductImageMapperMockTest extends BaseServiceTest {
    private ProductImageMapper imageMapper;
    @Before
    public void init(){
        //mock ProductImageMapper 代理实例
        imageMapper = mock(ProductImageMapper.class);
    }
    @Test
    public void testListImageByProductCode(){
        //构造，模据 调用 listByProductCode 返回对应的数据
        when(imageMapper.listByProductCode("abc")).thenReturn(mockImageList());
        List<String> images = imageMapper.listByProductCode("abc");
        assertEquals(2,images.size());
        images = imageMapper.listByProductCode("123");
        assertEquals(0,images.size());
    }
    private List<String> mockImageList(){
        List<String> objects = new ArrayList<>();
        objects.add("xxxxx.jpg");
        objects.add("yyyyy.jpg");
        return objects;
    }
}

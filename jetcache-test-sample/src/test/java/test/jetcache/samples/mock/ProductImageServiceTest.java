package test.jetcache.samples.mock;

import jetcache.samples.service.ImageRemoteService;
import jetcache.samples.service.ProductImageService;
import jetcache.samples.service.impl.ProductImageServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import test.jetcache.samples.BaseServiceTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class ProductImageServiceTest extends BaseServiceTest {
    //通过spring 注入
    @Autowired
    private ProductImageServiceImpl productImageService;

    @Before
    public void init() {
        //productImageService = mock(ProductImageServiceImpl.class);
        //仿造一个ImageRemoteService 实例，并设置到ProductImageServiceImpl里
        ImageRemoteService remoteService = mock(ImageRemoteService.class);
        productImageService.setImageRemoteService(remoteService);
    }
    @Test
    public void testListImageByProductCode() {
        //模拟接口返回值
        when(productImageService.listByProductCode2("abc"))
                .thenReturn(mockImageList());
        List<String> imageList = productImageService.listByProductCode2("abc");
        assert imageList.size() == 2;
    }
    private List<String> mockImageList() {
        List<String> objects = new ArrayList<>();
        objects.add("xxxxx.jpg");
        objects.add("yyyyy.jpg");
        return objects;
    }

    @Test
    public void testListImageByProductCode2() {
        ProductImageService spy = spy(productImageService);
        when(spy.listByProductCode2("123")).then(invocationOnMock -> {
            List<String> images = new ArrayList<>();
            images.add("xxxx");
            return images;
        });
        List<String> abc = spy.listByProductCode2("abc");
        assert abc.size() == 2;
        List<String> images = spy.listByProductCode2("123");
        assert images.size() == 1;
        reset(spy);
        when(spy.listByProductCode2("456")).thenReturn(mockImageList2());
        abc = spy.listByProductCode2("456");
        assert abc.size() == 2;
    }

    /**
     * 测试输入某个参，抛出某种异常
     */
    @Test(expected = RuntimeException.class)
    public void testThrowException() {
        ProductImageService spy = spy(productImageService);
        when(spy.listByProductCode2("123")).thenThrow(new RuntimeException("输入参数有误")).thenReturn(mockImageList());
        spy.listByProductCode2("123");
    }

    /**
     * 测试输入某个参，抛出某种异常
     */
    @Test()
    public void testThrowException2() {
        ProductImageService spy = spy(productImageService);
        when(spy.listByProductCode2("123")).thenThrow(new RuntimeException("输入参数有误"));
        verify(spy).listByProductCode2("123");
        when(spy.listByProductCode2("abc")).thenReturn(mockImageList());
        List<String> images = spy.listByProductCode2("abc");
        assertEquals(2,images.size());
    }



    private List<String> mockImageList2() {
        List<String> objects = new ArrayList<>();
        objects.add("aaa.jpg");
        objects.add("bbbb.jpg");
        return objects;
    }

}

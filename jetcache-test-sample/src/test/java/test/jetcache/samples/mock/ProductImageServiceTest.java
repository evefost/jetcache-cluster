package test.jetcache.samples.mock;

import com.lingzhi.dubhe.test.InjectUtils;
import jetcache.samples.dto.WeworkResponse;
import jetcache.samples.service.ImageRemoteService;
import jetcache.samples.service.ProductImageService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import test.jetcache.samples.BaseServiceTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class ProductImageServiceTest extends BaseServiceTest {
    //通过spring 注入
    @Autowired
    private ProductImageService productImageService;
    @Before
    public void init() throws IllegalAccessException {
        //productImageService = mock(ProductImageServiceImpl.class);
        //仿造一个ImageRemoteService 实例，并设置到ProductImageServiceImpl里
        ImageRemoteService remoteService = mock(ImageRemoteService.class);
        //不改变产生代码，注入mock实例
        InjectUtils.injectFields(productImageService, remoteService);
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
    public void mockRequestWework() throws IllegalAccessException {
        RestTemplate restTemplate = mock(RestTemplate.class);
        InjectUtils.injectFields(productImageService, restTemplate);
        //构造请求数参数
        String url = "http://xxx.com/xxxx/sss";
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.set("content-type", "application/json");
        HttpEntity requestEntity = new HttpEntity<>(null, headers);
        //模拟响应信息
        when(restTemplate.exchange(url, HttpMethod.POST, requestEntity, WeworkResponse.class)).then(invocationOnMock -> {
            //模拟企业微信响应时长
            Thread.sleep(200L);
            WeworkResponse response = new WeworkResponse();
            response.setUserId(123);
            response.setUserName("lingzhi");
            ResponseEntity<WeworkResponse> responseEntity = new ResponseEntity<>(response,HttpStatus.OK);
            return responseEntity;
        });
        WeworkResponse body1 = productImageService.requestWework();
        assert body1 != null;
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
        assert abc.size() == 0;
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
        assertEquals(2, images.size());
    }


    private List<String> mockImageList2() {
        List<String> objects = new ArrayList<>();
        objects.add("aaa.jpg");
        objects.add("bbbb.jpg");
        return objects;
    }

}

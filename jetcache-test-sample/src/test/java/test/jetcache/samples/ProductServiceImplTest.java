package test.jetcache.samples;

import com.alibaba.fastjson.JSON;
import jetcache.samples.dto.request.ProductRequest;
import jetcache.samples.dto.response.ProductResponse;
import jetcache.samples.service.ProductService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * ProductServiceImpl Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>9�� 3, 2020</pre>
 */
public class ProductServiceImplTest extends BaseServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private StringRedisTemplate template;

    /**
     * Method: getByProductCode(String productCode)
     */
    @Test
    public void testGetByProductCode() throws Exception {
//TODO: Test goes here... 
    }

    @Test
    public void testListProductByCode22() throws Exception {
        long id = Thread.currentThread().getId();
        List<String> keys = new ArrayList<>();
        keys.add("0001");
        keys.add("0002");
        keys.add("0003");
        keys.add("0004");
        keys.add("0005");
        keys.add("0005");
        keys.add("0006");
        keys.add("0007");
        template.opsForValue().multiGet(keys);
        ProductResponse byProductCode2 = productService.getByProductCode2("1000");
        byProductCode2 = productService.getByProductCode3("1000");
        byProductCode2 = productService.getByProductCode2("0008");
        byProductCode2 = productService.getByProductCode2("0008");
//        List<ProductResponse> productResponses3 = productService.listProduct3(request);
        // productService.batchDelete(request);
        System.out.println("sss");
    }

    @Test
    public void testListProductByCode2() throws Exception {
        long id = Thread.currentThread().getId();

        ProductResponse byProductCode2 = productService.getByProductCode2("1000");
        byProductCode2 = productService.getByProductCode3("1000");
        byProductCode2 = productService.getByProductCode2("0008");
        byProductCode2 = productService.getByProductCode2("0008");
//        List<ProductResponse> productResponses3 = productService.listProduct3(request);
        // productService.batchDelete(request);
        System.out.println("sss");
    }

    @Test
    public void testListProductByCode7() throws Exception {

        ProductResponse byProductCode2 = productService.getByProductCode7("1000");
        System.out.println("sss");
    }

    /**
     * Method: listProduct(ProductRequest request)
     */
    @Test
    public void testListProduct() throws Exception {
        ProductRequest request = buildRequest(2);
        List<ProductResponse> productResponses = productService.listProduct(request);
//        List<ProductResponse> productResponses3 = productService.listProduct3(request);
        // productService.batchDelete(request);
        System.out.println("sss");
    }

    /**
     * Method: listProduct2(ProductRequest request)
     */
    @Test
    public void testListProduct2() throws Exception {
        ProductRequest request = buildRequest(2);
        List<ProductResponse> productResponses = productService.listProduct2(request);
        productResponses = productService.listProduct3(request);
        request = buildRequest(5);
        List<ProductResponse> productResponses2 = productService.listProduct2(request);
        System.out.println(productResponses2);
        productService.batchDelete(request);
        System.out.println("sss");

    }

    /**
     * Method: listProduct3(ProductRequest request)
     */
    @Test
    public void testListProduct3() throws Exception {
        ProductRequest request = buildRequest(2);
        while (true){
            Thread.sleep(2000);
            try {
               template.opsForValue().set("aaaa","aa",12);
                System.out.println("============");
            }catch (Exception e){
                e.printStackTrace();
            }

            List<ProductResponse> productResponses = productService.listProduct3(request);
            System.out.println(JSON.toJSONString(productResponses));
        }

//        request = buildRequest(5);
//        List<ProductResponse> productResponses2 = productService.listProduct3(request);
//        productResponses2 = productService.listProduct3(request);
//        System.out.println(productResponses2);
//        productService.batchDelete(request);
//        System.out.println("sss");

    }

    @Test
    public void testListProduct4() throws Exception {
        ProductRequest request = buildRequest(2);
        List<ProductResponse> productResponses = productService.listProduct4(request.getProductCodes());
        request = buildRequest(5);
        List<ProductResponse> productResponses2 = productService.listProduct4(request.getProductCodes());
        System.out.println(productResponses2);
        productService.batchDelete(request);
        System.out.println("sss");

    }

    /**
     * 单个与批量组合测试
     * @throws Exception
     */
    @Test
    public void singleAdnBatch() throws Exception {
        ProductResponse byProductCode = productService.getByProductCode2("000");
        ProductRequest request = buildRequest(2);
        List<ProductResponse> productResponses = productService.listProduct4(request.getProductCodes());
        request = buildRequest(5);
        List<ProductResponse> productResponses2 = productService.listProduct4(request.getProductCodes());
        System.out.println(productResponses2);
        productResponses2 = productService.listProduct4(request.getProductCodes());
        productService.batchDelete(request);
        System.out.println("sss");

    }


    private ProductRequest buildRequest(int size) {
        ProductRequest request = new ProductRequest();
        request.setStoreId(123);
        ArrayList<String> productCodes = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            productCodes.add("00" + i);
        }
        request.setProductCodes(productCodes);
        return request;
    }

    @Test
    public void getProductByCode7(){
        long s = System.currentTimeMillis();
        ProductResponse sssss = productService.getByProductCode7("sssss");
        assert sssss!=null;
        long e = System.currentTimeMillis();
        System.out.println("耗时:"+(e-s));
    }


}

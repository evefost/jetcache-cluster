package test.jetcache.samples;

import jetcache.samples.dto.response.ProductResponse;
import jetcache.samples.dto.response.SkuResponse;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProxyReferenceTest {

    public static ThreadLocal<Map<String, TaskInfo>> methods = new ThreadLocal() {
        @Override
        protected Object initialValue() {
            return new HashMap<>();
        }
    };


    public static class TaskInfo {
        private Method method;
        private Object[] args;

        private Object result;

        public Object getResult() {
            return result;
        }

        public void setResult(Object result) {
            this.result = result;
        }

        public Method getMethod() {
            return method;
        }

        public void setMethod(Method method) {
            this.method = method;
        }

        public Object[] getArgs() {
            return args;
        }

        public void setArgs(Object[] args) {
            this.args = args;
        }
    }

    public static void main(String[] args) {
        Class<?>[] interfaces = ProxyReference.class.getInterfaces();
        ProxyReference reference = new ProxyReference();
        ProxyReferenceInterface proxyReferenceInterface = (ProxyReferenceInterface) Proxy.newProxyInstance(ProxyReferenceTest.class.getClassLoader(), interfaces, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getName().equals("getProduct")) {
                    ProductResponse productResponse = new ProductResponse();
                    return productResponse;
                }

                if (method.getName().equals("getSku")) {
                    SkuResponse targetSku = new SkuResponse();
                    TaskInfo info = new TaskInfo();
                    info.setResult(targetSku);
                    info.setMethod(method);
                    info.setArgs(args);
                    methods.get().put("sku", info);
                    return targetSku;
                }
                if (method.getName().equals("listSku")) {
                    List<SkuResponse> list = new ArrayList<>();
                    TaskInfo info = new TaskInfo();
                    info.setMethod(method);
                    info.setResult(list);
                    info.setArgs(args);
                    methods.get().put("listSku", info);
                    return list;
                }
                if (method.getName().equals("getImage")) {
                    TaskInfo skuMethodInfo = methods.get().get("sku");
                    Method skuMethod = skuMethodInfo.getMethod();
                    Object skuSource = skuMethod.invoke(reference, skuMethodInfo.getArgs());
                    Object sku = skuMethodInfo.getResult();
                    BeanUtils.copyProperties(skuSource, sku);

                    TaskInfo listSkuInfo = methods.get().get("listSku");
                    List result = (List) listSkuInfo.getResult();
                    Method listSkuMethod = listSkuInfo.getMethod();
                    List listSku = (List) listSkuMethod.invoke(reference, listSkuInfo.getArgs());

                    for(Object source:listSku){
                        SkuResponse targetElement = new SkuResponse();
                        BeanUtils.copyProperties(source, targetElement);
                        result.add(targetElement);
                    }

                    return method.invoke(reference, args);
                }
                return method.invoke(reference, args);
            }
        });
        ProductResponse product = proxyReferenceInterface.getProduct();
        SkuResponse sku = proxyReferenceInterface.getSku();
        List<SkuResponse> skuResponses = proxyReferenceInterface.listSku();
        String image = proxyReferenceInterface.getImage();
        System.out.println(product);

    }


}

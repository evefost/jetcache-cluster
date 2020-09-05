/**
 * Created on 2018/8/11.
 */
package jetcache.samples;

import com.alicp.jetcache.anno.config.EnableCreateCacheAnnotation;
import com.alicp.jetcache.anno.config.EnableMethodCache;
import com.jetcahe.support.config.EnableListCache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author <a href="mailto:areyouok@gmail.com">huangli</a>
 */
@SpringBootApplication(scanBasePackages = {"jetcache.samples","com.jetcache"})
@EnableMethodCache(basePackages = "jetcache")
@EnableListCache(basePackages = "jetcache.samples")
@EnableCreateCacheAnnotation

public class SpringBootApp {


    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(SpringBootApp.class);
        MyService myService = context.getBean(MyService.class);
        myService.createCacheDemo();
        myService.cachedDemo();
    }



}

/**
 * Created on 2018/8/11.
 */
package jetcache.samples;

import com.alicp.jetcache.anno.config.EnableCreateCacheAnnotation;
import com.alicp.jetcache.anno.config.EnableMethodCache;
import com.alicp.jetcache.autoconfigure.JetCacheAutoConfiguration;
import com.jetcahe.support.annotation.EnableExtendCache;
import jetcache.samples.aop.MultiTaskSynExecuteAspect;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:areyouok@gmail.com">huangli</a>
 */
@SpringBootApplication(scanBasePackages = {"jetcache.samples","com.jetcache","test.jectcache"})

@EnableExtendCache(basePackages = "jetcache.samples")
@EnableCreateCacheAnnotation
@EnableAspectJAutoProxy
public class SpringBootApp {


    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(SpringBootApp.class);
        MyService myService = context.getBean(MyService.class);
        myService.createCacheDemo();
        myService.cachedDemo();
    }


    @Bean
    RestTemplate restTemplate(){
        return new RestTemplate();
    }


    @Bean
    ExecutorService executorService(){
        return newExecutorService(100,1000);
    }

    private static ExecutorService newExecutorService(int poolSize, int queueSize) {
        LinkedBlockingQueue taskQueue = new LinkedBlockingQueue<>(queueSize);
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(poolSize, poolSize, 2, TimeUnit.SECONDS, taskQueue, new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable runnable) {
                return new Thread(runnable,"TestThread-"+threadIndex.getAndIncrement());
            }
        });
        return poolExecutor;
    }

    @Bean
    MultiTaskSynExecuteAspect multiTaskSynExecuteAspect(){
        return new MultiTaskSynExecuteAspect();
    }




}

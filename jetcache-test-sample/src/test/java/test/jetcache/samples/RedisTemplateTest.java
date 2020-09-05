package test.jetcache.samples;


import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

public class RedisTemplateTest extends BaseServiceTest {

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Test
    public void testGet(){
        String user = redisTemplate.opsForValue().get("user");
        redisTemplate.opsForValue().set("user","xieyang");
         user = redisTemplate.opsForValue().get("user");
        System.out.println("===="+user);
    }
}

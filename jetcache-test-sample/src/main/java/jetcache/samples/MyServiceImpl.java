/**
 * Created on 2018/8/11.
 */
package jetcache.samples;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.anno.CreateCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:areyouok@gmail.com">huangli</a>
 */
@Component
public class MyServiceImpl implements MyService {
    @CreateCache(name = "myServiceCache", expire = 60)
    private Cache<String, String> cache;

    @Autowired
    private UserService userService;

    @Override
    public void createCacheDemo() {
        cache.put("myKey", "myValue");
        String myValue = cache.get("myKey");
        System.out.println("get 'myKey' from cache:" + myValue);
    }

    @Override
    public void cachedDemo() {
        User user = new User();
        user.setUserId(1);
        User user2 = new User();
        user2.setUserId(2);
        List<User> pList = new ArrayList<>();
        pList.add(user);
        pList.add(user2);
//        List<User> users = userService.listUser(pList);
        List<User> users = userService.listUser2(123,pList);
        Tenant tenant = new Tenant();
        tenant.setStoreId(123);
        tenant.setUsers(pList);
//        List<User> users = userService.listUser(tenant);
        System.out.println(users);
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}

package test.jetcache.samples;

import jetcache.samples.Tenant;
import jetcache.samples.User;
import jetcache.samples.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * UserServiceImpl Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>9�� 2, 2020</pre>
 */
public class UserServiceImplTest extends BaseServiceTest {

    @Autowired
    private UserService userService;


    /**
     * Method: listUser(List<User> users)
     */
    @Test
    public void testListUserUsers() throws Exception {
        List<User> users = new ArrayList<>();
        User user = new User();
        user.setUserId(1);
        user.setUserName("aaa");
        User user2 = new User();
        user2.setUserId(2);
        user2.setUserName("bbb");
        users.add(user);
        users.add(user2);
        List<User> result = userService.listUser(users);
        assert result.size() > 0;
    }


    /**
     * Method: listUser(Integer storeId, List<User> users)
     */
    @Test
    public void testListUserForStoreIdUsers() throws Exception {
        List<User> users = new ArrayList<>();
        User user = new User();
        user.setUserId(1);
        user.setUserName("aaa");
        User user2 = new User();
        user2.setUserId(2);
        user2.setUserName("bbb");
        users.add(user);
        users.add(user2);
        List<User> result = userService.listUser(123, users);
        result = userService.listUser(123, users);
        userService.batchDelete(123, users);
        result = userService.listUser(123, users);
        userService.batchDelete(123, users);
        assert result.size() > 0;
    }

    /**
     * Method: listUser(Tenant tenant)
     */
    @Test
    public void testListUserTenant() throws Exception {
        List<User> users = new ArrayList<>();
        User user = new User();
        user.setUserId(1);
        user.setUserName("aaa");
        User user2 = new User();
        user2.setUserId(2);
        user2.setUserName("bbb");
        users.add(user);
        users.add(user2);
        Tenant tenant = new Tenant();
        tenant.setStoreId(1111);
        tenant.setUsers(users);
        List<User> result = userService.listUserByTenant(tenant);
        assert result.size() > 0;
    }


    @Test
    public void testBatchDelete() throws Exception {
        List<User> users = new ArrayList<>();
        User user = new User();
        user.setUserId(1);
        user.setUserName("aaa");
        User user2 = new User();
        user2.setUserId(2);
        user2.setUserName("bbb");
        users.add(user);
        users.add(user2);

        userService.batchDelete(123, users);
        int a=1;
        int b=2;
        fun(a,b);
        System.out.println("sss");
        assert true;
    }


    public void fun(int a, int b) {
        int temp = a;
        a = b;
        b = temp;
    }


} 

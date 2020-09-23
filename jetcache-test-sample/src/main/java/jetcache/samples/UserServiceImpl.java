/**
 * Created on 2018/8/11.
 */
package jetcache.samples;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:areyouok@gmail.com">huangli</a>
 */
@Repository
public class UserServiceImpl implements UserService {

    @Override
    public User loadUser(long userId) {
        System.out.println("load user: " + userId);
        User user = new User();
        user.setUserId(userId);
        user.setUserName("user" + userId);
        return user;
    }

    @Override
    public User loadUser(User user, Person person) {
        System.out.println("load user: " + user.getUserId());
        User user2 = new User();
        user2.setUserId(user.getUserId());
        user2.setUserName("user" + user.getUserId());
        return user2;
    }

    @Override
    public List<User> listUser(List<User> users) {
        System.out.println(users.size());
        List<User> list = new ArrayList<>();
        User user = new User();
        user.setUserId(1);
        User user2 = new User();
        user2.setUserId(2);
        list.add(user);
        list.add(user2);
        return list;
    }

    @Override
    public List<User> listUser2(Integer storeId, List<User> users) {
        List<User> list = new ArrayList<>();
        User user = new User();
        user.setUserId(1);
        user.setUserName("aaa");
        user.setNo("001");
        User user2 = new User();
        user2.setUserId(2);
        user2.setUserName("bbb");
        user2.setNo("002");
        list.add(user);
        list.add(user2);
        return list;
    }

    @Override
    public void batchDelete2(Integer storeId, List<User> users) {

    }

    @Override
    public List<User> listUserByTenant(Tenant tenant) {
        List<User> users = new ArrayList<>();
        User user = new User();
        user.setUserId(1);
        user.setUserName("aaa");
        User user2 = new User();
        user2.setUserId(2);
        user2.setUserName("bbb");
        users.add(user);
        users.add(user2);
        return users;
    }

    @Override
    public void batchDelete(Integer storeId, List<User> users) {
        System.out.println("batchDelete");
    }


}

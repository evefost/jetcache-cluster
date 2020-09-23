/**
 * Created on 2018/8/11.
 */
package jetcache.samples;

import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.jetcahe.support.annotation.ListCacheInvalidate;
import com.jetcahe.support.annotation.ListCached;


import java.util.List;


/**
 * @author
 */
public interface UserService {

    @Cached(name = "loadUser", localExpire = 100,cacheType = CacheType.LOCAL)
    User loadUser(long userId);

    @Cached(name = "loadUser",key = "#person.id+':'+#user.userId",localExpire = 100,cacheType = CacheType.LOCAL)
    User loadUser(User user, Person person);

    @ListCached(name = "loadUser",key = "#users[.userId",returnKey = "#returnResult[.userId",localExpire = 100,cacheType = CacheType.LOCAL)
    List<User> listUser(List<User> users);

    /**
     * "[" 标识该变量参与构建批量缓存key的列表
     * @param storeId
     * @param users
     * @return
     */
    @ListCached(name = "loadUser",key = "'storeId:'+#storeId+':userId:'+#users[.userId+':'+#users[.userName",expire = 100,cacheType = CacheType.REMOTE)
    List<User> listUser2(Integer storeId,List<User> users);
    /**
     * 批量删除数据
     * @param storeId
     * @param users
     */
    @ListCacheInvalidate(name = "loadUser",key = "'storeId:'+#storeId+':userId:'+#users[.userId+':'+#users[.userName")
    void  batchDelete2(Integer storeId,List<User> users);


    @ListCached(name = "loadUser",key = "#tenant.storeId+':'+#tenant.users[.userId",returnKey = "#tenant.storeId+':'+#returnList[.userId",expire = 100,cacheType = CacheType.REMOTE)
    List<User> listUserByTenant(Tenant tenant);

    /**
     * 批量删除数据
     * @param storeId
     * @param users
     */
    @ListCacheInvalidate(name = "loadUser",key = "#storeId+':'+#users[.userId+':'+#users[.userName")
    void  batchDelete(Integer storeId,List<User> users);

}

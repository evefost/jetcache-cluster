## 基于 jet-cache redis 缓存优化 

### 一.需求场景分析
    1.在实际项目实践中，存在大量需要批量查询缓存提高查询效率的业务场景，
    2.据于现有的开源客户端，通常的做法是：
        a.业务代码构建批量的缓存key,
        b.据key crc16算法匹配集群节点管道发送批量请求
        c.过滤出缓存中没有值的key对入的参数,从数据库查数据
    3.以上处理方式缺点:
        3.1 业务代码每次都要做写a,b,c相同的操作代码，
            工作量、复杂度、维护成本很自然上去了
        3.2 缓存代码完全侵入正常业务代，缓存系统是否正常可能会影响业务功能
    4. jetcaceh 现有问题
        4.1 只支持lettuce,不支持jedis客端集群,之前使用lettuce发现不少问题,暂时不打算使用
        4.2 不支持集群批量读写缓存
        4.3 缓存实例类必须实现Serializable，现在实际项目可能有部分类没实现该接口      
    
### 二.redis 批量缓存优化实现思路 
    1.分析参考jetcache实现源码，接下来基本据自身需求照抄了
    2.设计spring表达式如何去标识参与构建批量缓存key的入参列表
    3.据用户预先设定的spring el表式找出可操作入参列表达式
    4.据表达式操作入参，构建批量缓存key
    5.通过redis 管道查询key 对应的缓存
    6.过滤掉已有缓存入参列表元素
    7.调用业务代码从数据库查数据
    8.将库量读取的数据写入redis缓存
    9.将缓结果与查库结果合并，并返回
    10.还原过滤掉的参数
    
### 三. jet-cache jedis 客户端集群支持实现
    1.分析发现写缓存实现RedisCache，仅支持jedis单机操作
    2.只要实现一个类似RedisCache 支持集群的功能，替换掉现有实现RedisCache即可
    3.创建RedisClusterCache继承AbstractExternalCache 抽象
    4.配置把RedisCache 替换成 RedisClusterCache
    
### 四.优化结果
    1.业务代码只需一行即可读写缓存
    2.业务代码与缓存代码解藕
    3.缓存系统崩溃不影响业务功能
        
### 五.缓存穿透实现思路
    1.穿透Key计算=所有key-(缓存有值的key+db有值的key)
    2.....
   
### 六.关键知识点
    1.redis 群集管道
    2.sping el 表达式操作
    3.spring aop 代理实现
    4.java 反射、注解基本用法
    
###==============================
    SpringCacheContext#buildRemote()->GlobalCacheConfig
    GlobalCacheConfig#getRemoteCacheBuilders->CacheBuilder
    CacheBuilder#buildCache()
    RedisAutoConfiguration
    RedisAutoInit
    分析JedisClient 读写缓存是通过 key crc16 找到对应管道是写数据的
    
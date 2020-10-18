package jetcache.samples;


/**
 * context  接口用于匹配各种中间件
 * <p>
 *
 * @author xieyang
 * @version 1.0.0
 * @date 2020/3/4
 */
public interface Context<V> extends Cloneable {

    void put(String key, V value);

    V get(String key);

    default void remove(String key) {

    }

    default boolean in() {
        return true;
    }

    default void setIn(boolean in) {

    }

    Context<V> clone() throws CloneNotSupportedException;
}

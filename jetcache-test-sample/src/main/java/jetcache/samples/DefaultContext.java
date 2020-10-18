package jetcache.samples;

import java.util.HashMap;
import java.util.Map;

/**
 * 默认的 context 传递
 * <p>
 *
 * @author 谢洋
 * @version 1.0.0
 * @date 2020/5/9
 */
public class DefaultContext extends AbstractContext {

    private Map<String, String> container;


    public DefaultContext(boolean in) {
        this(in, new HashMap<>());
    }

    public DefaultContext(boolean in, Map<String, String> container) {
        super(in);
        this.container = container;
    }

    @Override
    public void put(String key, String value) {
        container.put(key, value);
    }

    @Override
    public String get(String key) {
        return container.get(key);
    }

    @Override
    public void remove(String key) {
        container.remove(key);
    }


    @Override
    public Context clone() {
        DefaultContext cloned = new DefaultContext(this.in);
        container.forEach((k, v) -> {
            cloned.put(k, v);
        });
        return cloned;
    }
}

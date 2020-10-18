package jetcache.samples;


/**
 * context
 * <p>
 *
 * @author xieyang
 * @version 1.0.0
 * @date 2020/3/4
 */
public abstract class AbstractContext implements Context<String> {

    protected boolean in;

    public AbstractContext(boolean in) {
        this.in = in;
    }

    @Override
    public boolean in() {
        return in;
    }

    @Override
    public void setIn(boolean in) {
        this.in = in;
    }

    @Override
    public Context<String> clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
}

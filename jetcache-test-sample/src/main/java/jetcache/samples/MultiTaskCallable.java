package jetcache.samples;

import java.util.concurrent.Callable;

public abstract class MultiTaskCallable implements Callable {

    protected String taskName;

    public MultiTaskCallable(String taskName) {
        this.taskName = taskName;
    }

}
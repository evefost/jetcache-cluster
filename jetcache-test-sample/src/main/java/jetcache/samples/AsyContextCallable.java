package jetcache.samples;



import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

/**
 * 异步线程信息续传
 *
 * @author xieyang
 * @date 20/3/6
 */
public abstract class AsyContextCallable<C extends Context<String>, V> extends AbstractContextInterceptor<C> implements Runnable {
    protected Callable<V> callable;
    protected C context;

    public AsyContextCallable() {
        this(null, (C) AbstractContextInterceptor.getDefaultContext());
    }

    public AsyContextCallable(Callable<V> callable) {
        this(callable, (C) AbstractContextInterceptor.getDefaultContext());
    }

    public AsyContextCallable(Callable<V> callable, C context) {
        this.callable = callable;
        this.context = context;
    }


    @Override
    public void run() {
        try {
            this.context.setIn(true);
            setContext(context);
            this.doCall();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            clearContext(this.context);
        }
    }

    @Override
    public void clearContext(C context) {
        super.clearContext(context);
    }


    /**
     * 实现业务代码方法
     *
     * @throws Exception
     */
    public abstract void doCall() throws Exception;


    /**
     * 多任务异步执行，无返回值
     *
     * @param <V>
     * @param executor
     * @param tasks
     */
    public static <V> void execute(Executor executor, Callable<V>... tasks) throws CloneNotSupportedException {
        execute(executor, AbstractContextInterceptor.getDefaultContext(), tasks);
    }

    /**
     * 多任务异步执行，无返回值
     *
     * @param executor
     * @param context  指定context
     * @param tasks
     * @param <V>
     */
    public static <V> void execute(Executor executor, Context<String> context, Callable<V>... tasks) throws CloneNotSupportedException {
        for (Callable call : tasks) {
            Context<String> cloneContext = context.clone();
            AsyContextCallable<Context<String>, V> callable = new AsyContextCallable<Context<String>, V>(call, cloneContext) {
                @Override
                public void doCall() throws Exception {
                    callable.call();
                }
            };
            executor.execute(callable);
        }

    }

    /**
     * 多任务异步执行，同步返回结果
     * 注意，执行任务返回结果类型必须相同
     *
     * @param executor
     * @param tasks
     * @param <V>
     * @return
     * @throws InterruptedException
     */
    public static <V> List<V> submit(Executor executor, Callable<V>... tasks) throws InterruptedException, CloneNotSupportedException {
        return submit(executor, AbstractContextInterceptor.getDefaultContext(), tasks);
    }


    /**
     * 多任务异步执行，同步返回结果
     * 注意，执行任务返回结果类型必须相同
     *
     * @param executor
     * @param tasks
     * @param <V>
     * @return
     * @throws InterruptedException
     */
    public static <V> List<V> submit(Executor executor, Context<String> context, Callable<V>... tasks) throws InterruptedException, CloneNotSupportedException {
        List<V> resultList = Collections.synchronizedList(new ArrayList(tasks.length));
        CountDownLatch countDownLatch = new CountDownLatch(tasks.length);
        for (Callable call : tasks) {
            Context<String> cloneContext = context.clone();
            AsyContextCallable<Context<String>, V> callable = new AsyContextCallable<Context<String>, V>(call, cloneContext) {
                @Override
                public void doCall() throws Exception {
                    try {
                        V result = callable.call();
                        if (result != null) {
                            resultList.add(result);
                        }
                    } finally {
                        countDownLatch.countDown();
                    }
                }
            };
            executor.execute(callable);
        }
        countDownLatch.await();
        return resultList;
    }



    /**
     * 多种不同类型的任务异步执行，同步返回不同类型的结果
     * 注意，执行任务可以各不相同，对应的返回结果通过map形式返回，任务名称为key，值为对应任务返回结果
     *
     * 指定的任务名称不能重复
     *
     * @param executor
     * @param tasks
     * @return 返回任务名称对应的结果
     * @throws InterruptedException
     * @throws CloneNotSupportedException
     */
    public static Map<String/*taskName*/,/*result*/Object> submitWithMultiTypeTask(Executor executor, MultiTaskCallable... tasks) throws InterruptedException, CloneNotSupportedException {
        return submitWithMultiTypeTask(executor, AbstractContextInterceptor.getDefaultContext(), tasks);
    }


    /**
     * 多种不同类型的任务异步执行，同步返回不同类型的结果
     * 注意，执行任务可以各不相同，对应的返回结果通过map形式返回，任务名称为key，值为对应任务返回结果
     *
     * 指定的任务名称不能重复
     * @param executor
     * @param context
     * @param tasks
     * @return
     * @throws InterruptedException
     * @throws CloneNotSupportedException
     */
    public static Map<String, Object> submitWithMultiTypeTask(Executor executor, Context<String> context, MultiTaskCallable... tasks) throws InterruptedException, CloneNotSupportedException {
        Set<String> taskNames = new HashSet<>();
        for(MultiTaskCallable callable:tasks){
            if(taskNames.contains(callable.taskName)){
                throw new RuntimeException("taskName["+callable.taskName+"]重复");
            }
            taskNames.add(callable.taskName);
        }
        Map<String, Object> multiResultMap = new ConcurrentHashMap<>(tasks.length);
        CountDownLatch countDownLatch = new CountDownLatch(tasks.length);
        for (Callable call : tasks) {
            Context<String> cloneContext = context.clone();
            AsyContextCallable<Context<String>, ?> callable = new AsyContextCallable<Context<String>, Object>(call, cloneContext) {
                @Override
                public void doCall() throws Exception {
                    try {
                        MultiTaskCallable multiCallable = (MultiTaskCallable) callable;
                        Object result = multiCallable.call();
                        multiResultMap.put(multiCallable.taskName, result);
                    } finally {
                        countDownLatch.countDown();
                    }
                }
            };
            executor.execute(callable);
        }
        countDownLatch.await();
        return multiResultMap;
    }


}

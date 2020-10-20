package test.jetcache.samples;

import org.junit.Test;

import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadTest {
    int threads = 100;
    Executor executor = Executors.newFixedThreadPool(threads);
    int counter = 0;

    //模拟并发访问统计
    @Test()
    public void testCount2() throws InterruptedException {
        //执行的任务次数
        int taskCount = 100000;
        MultiThreadTestUtils.TestResult executeResult = MultiThreadTestUtils.execute(executor, taskCount, () -> {
            //执行目标代码,非原子操作加锁
            synchronized (MultiThreadTest.class) {
                counter = counter + 1;
            }
            try {
                Thread.sleep(10 );
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        },threads);
        //校验统计结果
        assert taskCount == counter;
        System.out.println(executeResult);
    }

    @Test()
    public void testCount4() throws InterruptedException {
        Random random = new Random();
        MultiThreadTestUtils.TestResult execute = MultiThreadTestUtils.execute(executor, 10000l, () -> {
                    long startTime = System.currentTimeMillis();
                    try {
                        Thread.sleep(10 );
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    long e = System.currentTimeMillis();
                    long totalTime = (e-startTime);
//                    if (random.nextBoolean()) {
//                        //模拟出来
//                        throw new RuntimeException("出错了");
//                    }
                }
        ,threads);
        System.out.println(execute);
    }
}

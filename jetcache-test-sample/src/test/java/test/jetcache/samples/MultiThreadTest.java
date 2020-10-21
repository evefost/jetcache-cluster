package test.jetcache.samples;

import org.junit.Test;
import test.jetcache.samples.thread.MultiThreadTestUtils;
import test.jetcache.samples.thread.TestResult;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiThreadTest {
    static int threads = 100;
    static ExecutorService executor = Executors.newFixedThreadPool(threads);
    static int counter = 0;

    //模拟并发访问统计
//    @Test()
    public static void main(String[] args) throws InterruptedException {

        //执行的任务次数
        int taskCount = 1000002;
        TestResult executeResult = MultiThreadTestUtils.execute(200, 1000000, taskCount, () -> {
            //执行目标代码,非原子操作加锁
            synchronized (MultiThreadTest.class) {
                counter = counter + 1;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        //校验统计结果
        assert taskCount == counter;
        System.out.println(executeResult);
    }

    //
//    @Test()
//    public void testCount4() throws InterruptedException {
    public static void main2(String[] args) throws InterruptedException {


        Random random = new Random();
        TestResult execute = MultiThreadTestUtils.execute(200, 500000, 10000l, () -> {
                    synchronized (MultiThreadTest.class) {
                        counter = counter + 1;
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
        );
        System.out.println(execute);
    }
}

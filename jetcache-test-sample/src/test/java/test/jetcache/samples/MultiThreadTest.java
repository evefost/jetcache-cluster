package test.jetcache.samples;

import org.junit.Test;
import test.jetcache.samples.thread.MultiThreadTestUtils;
import test.jetcache.samples.thread.TestResult;

import java.util.Random;

public class MultiThreadTest {

    static int counter = 0;

    Random random = new Random();
    //模拟并发访问统计
    @Test()
    public void count() throws InterruptedException {
        //执行的任务次数
        int taskCount = 1000002;
        TestResult executeResult = MultiThreadTestUtils.execute(200, 1000000, 1000002, () -> {
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
        assert executeResult.getThrowableList().size()==0;

    }

    @Test()
    public void count2() throws InterruptedException {
        TestResult executeResult = MultiThreadTestUtils.execute(11, 10000, 200000l, () -> {
                    synchronized (MultiThreadTest.class) {
                        counter = counter + 1;
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
//                    if(random.nextInt(10)%3==0){
//                        throw new RuntimeException("模拟任务失败");
//                    }
                }
        ,1000);
        assert executeResult.getThrowableList().size()==0;
    }
}

package test.lingzhi.dubhe.test;

import com.lingzhi.dubhe.test.MultiThreadTestUtils;
import com.lingzhi.dubhe.test.TestResult;
import org.junit.Test;

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
        assert executeResult.getThrowableList().size() == 0;

    }

    @Test()
    public void count2() throws InterruptedException {
        //tps=threads*(1000/avgRequest)
        TestResult executeResult = MultiThreadTestUtils.execute(110, 10000, 40000l, 1200, () -> {
                    synchronized (MultiThreadTest.class) {
                        counter = counter + 1;
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
        );
        assert executeResult.getThrowableList().size() == 0;
    }


    @Test()
    public void count3() throws InterruptedException {
        //tps=threads*(1000/avgRequest)
        TestResult executeResult = MultiThreadTestUtils.execute(8, 10000, 2000000l, 1000, () -> {
                    for (int i = 0; i < 100000; i++) {
                        doSomething();
                    }
                }

        );
        assert executeResult.getThrowableList().size() == 0;
    }

    private static void doSomething() {
        StringBuilder sb = new StringBuilder();
        sb.append("aaaa").append("bbbbb");
        sb.toString();
    }

}

package test.jetcache.samples;

import org.junit.Test;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AccessCounterTest {

    Executor executor = Executors.newFixedThreadPool(100);

    private AccessCounter counter = new AccessCounter();

    @Test
    public void testCount() throws InterruptedException {
        int times = 10000;
        CountDownLatch latch = new CountDownLatch(times);
        CountDownLatch finishLatch = new CountDownLatch(times);
        Random random = new Random();
        for (int i = 0; i < times; i++) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        latch.await();
                        if (random.nextBoolean()) {
                            AMeth();
                        } else {
                            BMeth();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        finishLatch.countDown();
                    }
                }
            });
            latch.countDown();
        }
        finishLatch.await();
        assert counter.total == times;
    }

    //A接口
    private void AMeth() throws InterruptedException {
        counter.total++;
    }

    //b接口
    private void BMeth() throws InterruptedException {
        counter.total++;

    }
}

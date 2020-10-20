package test.jetcache.samples;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadTestUtils {

    /**
     *  按任务数来测试并发
     * @param executor
     * @param taskCount 任务数
     * @param target
     * @throws InterruptedException
     */
    public static TestResult  execute(Executor executor,int taskCount,Runnable target) throws InterruptedException {
        TestResult testResult = TestResult.build();
        AtomicInteger counter = new AtomicInteger(0);
        long s = System.currentTimeMillis();
        CountDownLatch executeLatch = new CountDownLatch(taskCount);
        CountDownLatch finishLatch = new CountDownLatch(taskCount);
        for (int i = 0; i < taskCount; i++) {
            executor.execute(() -> {
                try {
                    executeLatch.await();
                    target.run();
                } catch (Throwable e) {
                    testResult.addException(e);
                } finally {
                    finishLatch.countDown();
                    counter.incrementAndGet();
                }
            });
            executeLatch.countDown();
        }
        finishLatch.await();
        buildResult(testResult, taskCount, s);
        return testResult;
    }


    /**
     *
     * @param executor
     * @param executeTime 执行时长，单位ms
     * @param target
     * @throws InterruptedException
     */
    public static TestResult  execute(Executor executor,long executeTime,Runnable target) throws InterruptedException {
        TestResult testResult = TestResult.build();
        AtomicInteger counter = new AtomicInteger(0);
        Timer nTimer = new Timer();
        final Boolean[] run = {true};
        nTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                //停目执行任务
                run[0] = false;
            }
        }, executeTime);

        long s = System.currentTimeMillis();
        while (run[0]) {
            executor.execute(() -> {
                try {
                    target.run();
                }catch (Throwable throwable){
                    testResult.addException(throwable);
                }finally {
                    counter.incrementAndGet();
                }
            });
        }
        buildResult(testResult, counter.get(), s);
        return testResult;
    }

    private static void buildResult(TestResult testResult, int taskCount, long startTime) {
        long e = System.currentTimeMillis();
        long totalTime = (e-startTime);
        long avg = totalTime/taskCount;
        testResult.setTotalExecuteTime(totalTime);
        testResult.setAverageExecuteTime(avg);
        testResult.setTotalTask(taskCount);
    }


    public static class TestResult{

        /**
         * 总的执行时长
         */
        private long totalExecuteTime;

        /**
         * 每个任务平均的执行时长
         */
        private long averageExecuteTime;

        /**
         * 执行的任务个数
         */
        private int  totalTask;
        /**
         * 任务异常信息
         */
        private List<Throwable> throwableList;


        public int getTotalTask() {
            return totalTask;
        }

        public void setTotalTask(int totalTask) {
            this.totalTask = totalTask;
        }

        public long getTotalExecuteTime() {
            return totalExecuteTime;
        }

        public void setTotalExecuteTime(long totalExecuteTime) {
            this.totalExecuteTime = totalExecuteTime;
        }

        public long getAverageExecuteTime() {
            return averageExecuteTime;
        }

        public void setAverageExecuteTime(long averageExecuteTime) {
            this.averageExecuteTime = averageExecuteTime;
        }

        public List<Throwable> getThrowableList() {
            return throwableList;
        }

        public void setThrowableList(List<Throwable> throwableList) {
            this.throwableList = throwableList;
        }

        public void addException(Throwable throwable){
            throwableList.add(throwable);
        }

        public static TestResult build(){
            TestResult testResult = new TestResult();
            List<Throwable> throwableList = Collections.synchronizedList(new LinkedList());
            testResult.setThrowableList(throwableList);
            return testResult;
        }

        @Override
        public String toString() {
            return "测试执行结果{" +
                    "\n总执行时间=" + totalExecuteTime +
                    "\n单次任务平均执行时间=" + averageExecuteTime +
                    "\n总任务数=" + totalTask +
                    "\n执行异常次数=" + throwableList.size() +
                    '}';
        }
    }
}

package test.jetcache.samples;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MultiThreadTestUtils {

    /**
     *  按任务数来测试并发
     * @param executor
     * @param taskCount 任务数
     * @param target
     * @throws InterruptedException
     */
    public static TestResult  execute(Executor executor,int taskCount,Runnable target,int threads) throws InterruptedException {
        TestResult testResult = TestResult.build();
        AtomicInteger counter = new AtomicInteger(0);
        AtomicLong actualTotalTime = new AtomicLong(0);
        long s = System.currentTimeMillis();
        CountDownLatch executeLatch = new CountDownLatch(taskCount);
        CountDownLatch finishLatch = new CountDownLatch(taskCount);
        for (int i = 0; i < taskCount; i++) {
            executor.execute(() -> {
                long st = System.currentTimeMillis();
                try {
                    executeLatch.await();
                    target.run();
                } catch (Throwable e) {
                    testResult.addException(e);
                } finally {
                    finishLatch.countDown();
                    counter.incrementAndGet();
                    long et = System.currentTimeMillis();
                    long usedTime = et-st;
                    actualTotalTime.getAndAdd(usedTime);
                }
            });
            executeLatch.countDown();
        }
        finishLatch.await();
        buildResult(testResult, taskCount, s,threads,actualTotalTime.get());
        return testResult;
    }


    /**
     *
     * @param executor
     * @param executeTime 执行时长，单位ms
     * @param target
     * @throws InterruptedException
     */
    public static TestResult  execute(Executor executor,long executeTime,Runnable target,int threads) throws InterruptedException {
        TestResult testResult = TestResult.build();
        AtomicInteger counter = new AtomicInteger(0);
        AtomicLong actualTotalTime = new AtomicLong(0);
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
                long st = System.currentTimeMillis();
                try {
                    target.run();
                }catch (Throwable throwable){
                    testResult.addException(throwable);
                }finally {
                    counter.incrementAndGet();
                    long et = System.currentTimeMillis();
                    long usedTime = et-st;
                    actualTotalTime.getAndAdd(usedTime);
                }
            });
        }
        buildResult(testResult, counter.get(), s,threads,actualTotalTime.get());
        return testResult;
    }

    private static void buildResult(TestResult testResult, int taskCount, long startTime,int threads,long actualTotalTime) {
        long e = System.currentTimeMillis();
        long totalExecuteTime = (e-startTime);
        long avg = totalExecuteTime/(taskCount/threads);
        long avgActualTime = actualTotalTime/(taskCount);
        long totalInvalidTime=totalExecuteTime-avg*taskCount/threads;
        testResult.setTotalExecuteTime(totalExecuteTime);
        testResult.setAverageExecuteTime(avg);
        testResult.setTotalTask(taskCount);
        testResult.setAvgActualTime(avgActualTime);
        testResult.setTotalInvalidTime(totalInvalidTime);
    }


    public static class TestResult{

        /**
         * 执行的任务个数
         */
        private int  totalTask;


        /**
         * 总的执行时长
         */
        private long totalExecuteTime;

        /**
         * 每个任务平均的执行时长
         */
        private long averageExecuteTime;

        /**
         * 每个任务实际平均时长
         */
        private long avgActualTime;


        /**
         * 总无效时间(cpu上下文切)
         */
        private long totalInvalidTime;


        /**
         * 任务异常信息
         */
        private List<Throwable> throwableList;



        public long getTotalInvalidTime() {
            return totalInvalidTime;
        }

        public void setTotalInvalidTime(long totalInvalidTime) {
            this.totalInvalidTime = totalInvalidTime;
        }

        public long getAvgActualTime() {
            return avgActualTime;
        }

        public void setAvgActualTime(long avgActualTime) {
            this.avgActualTime = avgActualTime;
        }

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
            String usedPercent = String.format("cpu有效利用率=%.2f", (float) (totalExecuteTime - totalInvalidTime) / totalExecuteTime);
            return "测试执行结果{" +
                    "\n总任务数=" + totalTask +
                    "\n总执行时长=" + totalExecuteTime+"ms"+
                    "\ncpu上下文切换总耗时=" + totalInvalidTime+"ms"+usedPercent+
                    "\n单次任务平均执行时长=" + averageExecuteTime+"ms(实际有效执行时长:"+avgActualTime+"ms)"+
                    "\n单次任务平均阻塞时长=" + (averageExecuteTime-avgActualTime)+"ms)"+
                    "\n执行异常次数=" + throwableList.size() +
                    '}';
        }
    }
}

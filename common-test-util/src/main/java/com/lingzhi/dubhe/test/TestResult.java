package com.lingzhi.dubhe.test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author xieyang
 */
public class TestResult {

    /**
     * 执行的任务个数
     */
    private AtomicInteger taskCounter;


    /**
     * 总的执行时长
     */
    private long totalExecuteTime;

    /**
     * 总的执行时长
     */
    private AtomicLong actualTotalTime;

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

    private AtomicInteger rejectCount;

    private int submitTps;

    private int workThreads;

    public int getWorkThreads() {
        return workThreads;
    }

    public void setWorkThreads(int workThreads) {
        this.workThreads = workThreads;
    }

    public int getSubmitTps() {
        return submitTps;
    }

    public void setSubmitTps(int submitTps) {
        this.submitTps = submitTps;
    }

    private BlockingQueue<Runnable> taskQueue;

    public BlockingQueue<Runnable> getTaskQueue() {
        return taskQueue;
    }

    public void setTaskQueue(BlockingQueue<Runnable> taskQueue) {
        this.taskQueue = taskQueue;
    }

    public AtomicInteger getRejectCount() {
        return rejectCount;
    }

    public void setRejectCount(AtomicInteger rejectCount) {
        this.rejectCount = rejectCount;
    }

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

    public AtomicInteger getTaskCounter() {
        return taskCounter;
    }

    public AtomicLong getActualTotalTime() {
        return actualTotalTime;
    }

    public void setActualTotalTime(AtomicLong actualTotalTime) {
        this.actualTotalTime = actualTotalTime;
    }

    public void setTaskCounter(AtomicInteger taskCounter) {
        this.taskCounter = taskCounter;
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

    public void addException(Throwable throwable) {
        throwableList.add(throwable);
    }

    public static TestResult build() {
        TestResult testResult = new TestResult();
        List<Throwable> throwableList = Collections.synchronizedList(new LinkedList());
        testResult.setThrowableList(throwableList);
        testResult.setRejectCount(new AtomicInteger(0));
        return testResult;
    }


}
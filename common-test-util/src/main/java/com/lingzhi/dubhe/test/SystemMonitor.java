package com.lingzhi.dubhe.test;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;

/**
 * @author xieyang
 */
public class SystemMonitor {

    private static SystemMonitor instance = new SystemMonitor();

    private OperatingSystemMXBean osMxBean;

    private ThreadMXBean threadBean;

    private long preTime = System.nanoTime();

    private long preUsedTime = 0;

    private static boolean running =true;

    private static int threads;

    private static TestResult testResult;

    private long startTime=System.currentTimeMillis();

    private SystemMonitor() {
        osMxBean = ManagementFactory.getOperatingSystemMXBean();
        threadBean = ManagementFactory.getThreadMXBean();
    }

    public static SystemMonitor getInstance() {
        return instance;
    }

    public double getCpuUsed() {
        long totalTime = 0;
        long[] allThreadIds = threadBean.getAllThreadIds();
        threads = allThreadIds.length;
        for (long id : allThreadIds) {
            totalTime += threadBean.getThreadCpuTime(id);
        }
        long currentTime = System.nanoTime();
        long usedTime = totalTime - preUsedTime;
        long totalPassedTime = currentTime - preTime;
        preTime = currentTime;
        preUsedTime = totalTime;
        testResult.setTotalExecuteTime(System.currentTimeMillis()-startTime);
        return (((double) usedTime) / totalPassedTime / osMxBean.getAvailableProcessors()) * 100;
    }

    public static void start(TestResult result){
        testResult= result;
        Thread thread = new Thread(() -> {
            while (running) {
                print();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


        });
        thread.start();
    }

    private static void print(){
        String title = "|cpu使用|工作线程|已执行任务 |拒绝任务 | 失败任务 |任务平均耗时|累计运行时长";

        String cpu = String.format("| %.2f", SystemMonitor.getInstance().getCpuUsed());

        int taskCount = testResult.getTaskCounter().get()==0?1:testResult.getTaskCounter().get();
        long avgTaskTime =  testResult.getActualTotalTime().get()/taskCount;
        String workThread = String.format(" | %d", testResult.getWorkThreads());
        String rejectCount =  String.format("   | %d  ", testResult.getRejectCount().get());
        String failTask =  String.format("   | %d ", testResult.getThrowableList().size());
        String avgTaskTimeStr =  String.format("| %d ms", avgTaskTime);
        String executeTime =  String.format("    | %d ms", testResult.getTotalExecuteTime());
        String des = cpu+workThread+"   |"+testResult.getTaskCounter().get()+rejectCount+failTask+avgTaskTimeStr+executeTime;
        float currentTps = 0;
        if(testResult.getTotalExecuteTime()/1000<1){
            currentTps=0;
        }else {
            currentTps = taskCount*1000f/testResult.getTotalExecuteTime();
        }
        String currentTpsStr = String.format("%.1f", currentTps);

        float mayTps=0;
        if(avgTaskTime>0){
            mayTps=((float)1000/avgTaskTime)*testResult.getWorkThreads();
        }
        String mayTpsStr = String.format("%.1f", mayTps);
        float submitTps = (taskCount+testResult.getTaskQueue().size())*1000/testResult.getTotalExecuteTime();
        String submitTpsStr = String.format("%.1f", submitTps);
        String subScript = "等待任务["+testResult.getTaskQueue().size()+"]当前提交tps["+submitTpsStr+"]当前处理tps["+currentTpsStr+"]估算处理能力tps约["+mayTpsStr+"]";

        StringBuilder sb=new StringBuilder();
        for(int i=0;i<des.length()+15;i++){
            sb.append('-');
        }
        System.out.println(sb.toString());
        System.out.println(title);
        System.out.println(des);
        System.out.println(subScript);
        if(!running){
            System.out.println("执行结束...");
        }
    }

    public static void stop(){
        running = false;
        print();
    }
}

/*
 * 深圳市灵智数科有限公司版权所有.
 */
package jetcache.samples.aop;

import jetcache.samples.AsyContextCallable;
import jetcache.samples.MultiTaskCallable;
import jetcache.samples.annotation.AsynTask;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步任务同步返回
 * 1.识别入口，构建context
 * 2.创建任务
 * 3.给当前任务创建一个结果，并持有该结果
 * 4.判断是否为最后一个子任务
 * 5.执行所有了任务
 * 6.处理所有子任务结果
 * 7.返加最后一个任务真实结果
 *
 * @author xieyang
 */

@Aspect
@Component
public class AsyTaskOperateAspect {

    public static ThreadLocal<TaskContext> taskContextHolder = new ThreadLocal<>();

    @Pointcut("@annotation(jetcache.samples.annotation.AsynTask))")
    public void operate() {
    }

    @Around("operate()")
    public Object aroundOperate(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sign = (MethodSignature) pjp.getSignature();
        Method method = sign.getMethod();
        AsynTask annotation = AnnotationUtils.getAnnotation(method, AsynTask.class);
        TaskContext taskContext = taskContextHolder.get();
        boolean isTaskEntry = false;
        TaskInfo subTask = null;
        if (StringUtils.isEmpty(annotation.parentName())) {
            isTaskEntry = true;
            createContext(method, annotation);
        } else if(taskContext != null){
            subTask = parseSubTask(pjp, annotation);
        }
        try {
            if (isTaskEntry || taskContext==null) {
                return pjp.proceed();
            }
            return invokeSubTask(pjp, subTask);
        } finally {
            if (isTaskEntry) {
                taskContextHolder.remove();
            }
        }
    }


    private Object invokeSubTask(ProceedingJoinPoint pjp, TaskInfo task) throws Throwable {
        MethodSignature sign = (MethodSignature) pjp.getSignature();
        Method method = sign.getMethod();
        TaskContext taskContext = taskContextHolder.get();
        Class<?> returnType = method.getReturnType();
        Object returnResult = null;
        if (returnType.isAssignableFrom(List.class)) {
            returnResult = new ArrayList<>();
        } else if (returnType.isAssignableFrom(Map.class)) {
            returnResult = new HashMap();
        } else {
            returnResult = returnType.newInstance();
        }
        task.setResult(returnResult);
        if (!task.isLastTask) {
            return returnResult;
        }
        //等到最后一个子任务再执行
        MultiTaskCallable[] allSubTask = createAllSubTask(taskContext);
        AsyContextCallable.submitWithMultiTypeTask(executorService(), allSubTask);
        //返回最后一个任务的结果
        return taskContext.getLastSubTaskInfo().getResult();

    }


    private MultiTaskCallable[] createAllSubTask(TaskContext taskContext) {
        List<TaskInfo> subTaskInfoList = taskContext.getSubTaskInfoList();
        MultiTaskCallable[] taskList = new MultiTaskCallable[subTaskInfoList.size()];
        for (int i = 0; i < taskList.length; i++) {
            taskList[i] = new SubTask("multiSycTask" + i, subTaskInfoList.get(i));
        }
        return taskList;
    }

    static class SubTask extends MultiTaskCallable {
        TaskInfo taskInfo;

        public SubTask(String taskName, TaskInfo taskInfo) {
            super(taskName);
            this.taskInfo = taskInfo;
        }


        @lombok.SneakyThrows
        @Override
        public Object call() throws Exception {
            ProceedingJoinPoint joinPoint = taskInfo.getPjp();
            Object result = joinPoint.proceed();
            Object targetResult = taskInfo.getResult();
            if (result instanceof List) {
                List listResult = (List) result;
                List targetListResult = (List) taskInfo.getResult();
                for (Object obj : listResult) {
                    targetListResult.add(obj);
                }
            } else {
                BeanUtils.copyProperties(result, targetResult);
            }
            return targetResult;
        }
    }

    private TaskInfo parseSubTask(ProceedingJoinPoint pjp, AsynTask annotation) {

        MethodSignature sign = (MethodSignature) pjp.getSignature();
        Method method = sign.getMethod();
        TaskContext taskContext = taskContextHolder.get();
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setArgs(pjp.getArgs());
        taskInfo.setMethod(method);
        taskInfo.setPjp(pjp);
        taskContext.getSubTaskInfoList().add(taskInfo);
        if (taskContext.getTotalSubTask() == taskContext.getSubTaskInfoList().size()) {
            taskInfo.setLastTask(true);
            taskContext.setLastSubTaskInfo(taskInfo);
        }

        return taskInfo;
    }


    private void createContext(Method method, AsynTask task) {
        TaskContext context = new TaskContext();
        taskContextHolder.set(context);
        context.setTotalSubTask(task.subTasks());
        context.setHashParent(true);
        context.setSubTaskInfoList(new ArrayList<>(task.subTasks()));
    }


    static class TaskContext {

        private int totalSubTask;

        private boolean hashParent;


        private List<TaskInfo> subTaskInfoList;

        private TaskInfo lastSubTaskInfo;

        public boolean isHashParent() {
            return hashParent;
        }

        public void setHashParent(boolean hashParent) {
            this.hashParent = hashParent;
        }

        public TaskInfo getLastSubTaskInfo() {
            return lastSubTaskInfo;
        }

        public void setLastSubTaskInfo(TaskInfo lastSubTaskInfo) {
            this.lastSubTaskInfo = lastSubTaskInfo;
        }

        public int getTotalSubTask() {
            return totalSubTask;
        }

        public void setTotalSubTask(int totalSubTask) {
            this.totalSubTask = totalSubTask;
        }

        public List<TaskInfo> getSubTaskInfoList() {
            return subTaskInfoList;
        }

        public void setSubTaskInfoList(List<TaskInfo> subTaskInfoList) {
            this.subTaskInfoList = subTaskInfoList;
        }
    }


    static class TaskInfo {

        private String name;

        private Method method;

        private Object[] args;

        private Object result;

        private boolean isLastTask;

        private ProceedingJoinPoint pjp;

        public ProceedingJoinPoint getPjp() {
            return pjp;
        }

        public void setPjp(ProceedingJoinPoint pjp) {
            this.pjp = pjp;
        }

        public boolean isLastTask() {
            return isLastTask;
        }

        public void setLastTask(boolean lastTask) {
            isLastTask = lastTask;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Object getResult() {
            return result;
        }

        public void setResult(Object result) {
            this.result = result;
        }

        public Method getMethod() {
            return method;
        }

        public void setMethod(Method method) {
            this.method = method;
        }

        public Object[] getArgs() {
            return args;
        }

        public void setArgs(Object[] args) {
            this.args = args;
        }
    }

    private ExecutorService executorService;


    ExecutorService executorService() {
        if (executorService != null) {
            return executorService;
        }
        return newExecutorService(100, 1000);
    }

    private static ExecutorService newExecutorService(int poolSize, int queueSize) {
        LinkedBlockingQueue taskQueue = new LinkedBlockingQueue<>(queueSize);
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(poolSize, poolSize, 2, TimeUnit.SECONDS, taskQueue, new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable runnable) {
                return new Thread(runnable, "TestThread-" + threadIndex.getAndIncrement());
            }
        });
        return poolExecutor;
    }
}

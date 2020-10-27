/*
 * 深圳市灵智数科有限公司版权所有.
 */
package jetcache.samples.aop;

import jetcache.samples.AsyContextCallable;
import jetcache.samples.MultiTaskCallable;
import jetcache.samples.annotation.MultiSyncSubTask;
import jetcache.samples.annotation.MultiSyncTaskEntry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

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
public class MultiTaskSynExecuteAspect implements ApplicationContextAware {

    public static ThreadLocal<TaskContext> taskContextHolder = new ThreadLocal<>();

    @Pointcut("@annotation(jetcache.samples.annotation.MultiSyncTaskEntry)")
    public void multiTaskEntry() {
    }

    @Pointcut("@annotation(jetcache.samples.annotation.MultiSyncSubTask)")
    public void subTask() {
    }

    @Around("multiTaskEntry()")
    public Object executeMultiTask(ProceedingJoinPoint pjp) throws Throwable {
        createContext(pjp);
        try {
            return pjp.proceed();
        } finally {
            taskContextHolder.remove();
        }
    }

    @Around("subTask()")
    public Object executeSubTask(ProceedingJoinPoint pjp) throws Throwable {
        TaskContext taskContext = taskContextHolder.get();
        if (taskContext == null) {
            //子任务前没有父任务入口，按原来的方式执行
            return pjp.proceed();
        }
        SubTaskInfo subTask = parseSubTask(pjp,taskContext);
        return invokeSubTask(pjp, subTask);
    }


    private Object invokeSubTask(ProceedingJoinPoint pjp, SubTaskInfo task) throws Throwable {
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
        AsyContextCallable.submitWithMultiTypeTask(getExecute(taskContext.getThreadPoolName()), allSubTask);
        //返回最后一个任务的结果
        return taskContext.getLastSubSubTaskInfo().getResult();

    }

    private ExecutorService getExecute(String threadPoolName) {
        ExecutorService executorService = (ExecutorService) applicationContext.getBean(threadPoolName);
        return executorService;
    }


    private MultiTaskCallable[] createAllSubTask(TaskContext taskContext) {
        List<SubTaskInfo> subSubTaskInfoList = taskContext.getSubSubTaskInfoList();
        MultiTaskCallable[] taskList = new MultiTaskCallable[subSubTaskInfoList.size()];
        for (int i = 0; i < taskList.length; i++) {
            taskList[i] = new SubTask("multiSycTask" + i, subSubTaskInfoList.get(i));
        }
        return taskList;
    }

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    static class SubTask extends MultiTaskCallable {
        SubTaskInfo SubTaskInfo;

        public SubTask(String taskName, SubTaskInfo SubTaskInfo) {
            super(taskName);
            this.SubTaskInfo = SubTaskInfo;
        }
        @lombok.SneakyThrows
        @Override
        public Object call() throws Exception {
            ProceedingJoinPoint joinPoint = SubTaskInfo.getPjp();
            Object result = joinPoint.proceed();
            Object targetResult = SubTaskInfo.getResult();
            if (result instanceof List) {
                List listResult = (List) result;
                List targetListResult = (List) SubTaskInfo.getResult();
                for (Object obj : listResult) {
                    targetListResult.add(obj);
                }
            } else {
                BeanUtils.copyProperties(result, targetResult);
            }
            return targetResult;
        }
    }

    private SubTaskInfo parseSubTask(ProceedingJoinPoint pjp,TaskContext taskContext) {
        SubTaskInfo SubTaskInfo = new SubTaskInfo();
        SubTaskInfo.setPjp(pjp);
        taskContext.getSubSubTaskInfoList().add(SubTaskInfo);
        if (taskContext.getTotalSubTask() == taskContext.getSubSubTaskInfoList().size()) {
            SubTaskInfo.setLastTask(true);
            taskContext.setLastSubSubTaskInfo(SubTaskInfo);
        }
        return SubTaskInfo;
    }


    private void createContext(ProceedingJoinPoint pjp) {
        MethodSignature sign = (MethodSignature) pjp.getSignature();
        Method method = sign.getMethod();
        MultiSyncTaskEntry taskEntry = AnnotationUtils.getAnnotation(method, MultiSyncTaskEntry.class);
        TaskContext context = new TaskContext();
        taskContextHolder.set(context);
        context.setTotalSubTask(taskEntry.subTaskCount());
        context.setSubSubTaskInfoList(new ArrayList<>(taskEntry.subTaskCount()));
        context.setThreadPoolName(taskEntry.threadPoolName());
    }


    static class TaskContext {

        private int totalSubTask;

        private String threadPoolName;

        public String getThreadPoolName() {
            return threadPoolName;
        }

        public void setThreadPoolName(String threadPoolName) {
            this.threadPoolName = threadPoolName;
        }

        private List<SubTaskInfo> subSubTaskInfoList;

        private SubTaskInfo lastSubSubTaskInfo;

        public SubTaskInfo getLastSubSubTaskInfo() {
            return lastSubSubTaskInfo;
        }

        public void setLastSubSubTaskInfo(SubTaskInfo lastSubSubTaskInfo) {
            this.lastSubSubTaskInfo = lastSubSubTaskInfo;
        }

        public int getTotalSubTask() {
            return totalSubTask;
        }

        public void setTotalSubTask(int totalSubTask) {
            this.totalSubTask = totalSubTask;
        }

        public List<SubTaskInfo> getSubSubTaskInfoList() {
            return subSubTaskInfoList;
        }

        public void setSubSubTaskInfoList(List<SubTaskInfo> subSubTaskInfoList) {
            this.subSubTaskInfoList = subSubTaskInfoList;
        }
    }


    static class SubTaskInfo {

        private String name;

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
    }


}

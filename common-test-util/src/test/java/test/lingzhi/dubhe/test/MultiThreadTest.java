package test.lingzhi.dubhe.test;

import com.lingzhi.dubhe.test.MultiThreadTestUtils;
import com.lingzhi.dubhe.test.TestResult;
import javassist.*;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        TestResult executeResult = MultiThreadTestUtils.execute(11, 1000, 2000000l, 1150, () -> {
                    synchronized (MultiThreadTest.class) {
                        counter = counter + 1;
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        Thread.sleep(10);
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

    @Test()
    public void count4() throws InterruptedException, Exception {
//        while (true) {
        for(int i=0;i<10000000;i++){
            Thread.sleep(1);
            ClassPool cp = ClassPool.getDefault();
            CtClass ctClass = cp.makeClass("com.lingzhi.JavassistClass"+i);

            StringBuffer body = null;
            //参数  1：属性类型  2：属性名称  3：所属类CtClass
            CtField ctField = new CtField(cp.get("java.lang.String"), "name", ctClass);
            ctField.setModifiers(Modifier.PRIVATE);
            //设置name属性的get set方法
            ctClass.addMethod(CtNewMethod.setter("setName", ctField));
            ctClass.addMethod(CtNewMethod.getter("getName", ctField));
            ctClass.addField(ctField, CtField.Initializer.constant("default"));

            //参数  1：参数类型   2：所属类CtClass
            CtConstructor ctConstructor = new CtConstructor(new CtClass[]{}, ctClass);
            body = new StringBuffer();
            body.append("{\n name=\"me\";\n}");
            ctConstructor.setBody(body.toString());
            ctClass.addConstructor(ctConstructor);

            //参数：  1：返回类型  2：方法名称  3：传入参数类型  4：所属类CtClass
            CtMethod ctMethod = new CtMethod(CtClass.voidType, "execute", new CtClass[]{}, ctClass);
            ctMethod.setModifiers(Modifier.PUBLIC);
            body = new StringBuffer();
            body.append("{\n System.out.println(name);");
            body.append("\n System.out.println(\"execute ok\");");
            body.append("\n return ;");
            body.append("\n}");
            ctMethod.setBody(body.toString());
            ctClass.addMethod(ctMethod);
            Class<?> c = ctClass.toClass();
            Object o = c.newInstance();
            Method method = o.getClass().getMethod("execute", new Class[]{});
            //调用字节码生成类的execute方法
            method.invoke(o, new Object[]{});
       }
    }



}

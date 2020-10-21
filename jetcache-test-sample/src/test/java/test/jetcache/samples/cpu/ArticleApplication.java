package test.jetcache.samples.cpu;
 
public class ArticleApplication {
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 2; i++) {
            new Thread(() -> {
                while (true) {
                    long bac = 1000000;
                    bac = bac >> 1;
                }
            }).start();;
        }
        while (true) {
            Thread.sleep(5000);
            System.out.println(CPUMonitorCalc.getInstance().getProcessCpu());
        }
        
    }
}
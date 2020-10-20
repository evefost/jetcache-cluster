package test.jetcache.samples;

public class AccessCounter {
    /**
     * 访问量计数据器
     */
    private   int total;

    public int increase(){
        total++;
        return total;
    }
}

package xyz.lizhaorong.connection;

import java.sql.Connection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionPoolTest {

    static ConnectionPool pool = new ConnectionPool(10);
    // 保证所有的ConnectionRunner会同时开始
    static CountDownLatch start = new CountDownLatch(1);
    // main线程会等待所有runner结束后才会继续执行
    static CountDownLatch end;

    public static void main(String[] args) throws InterruptedException {
        // 从10-50，多次尝试测试性能
        int threadCount = 50;
        end = new CountDownLatch(threadCount);
        final int count = 20;

        AtomicInteger got = new AtomicInteger();
        AtomicInteger notgot = new AtomicInteger();

        for(int i =0; i < threadCount; i++){
            Thread thread = new Thread(new ConnectionRunner(count,got,notgot),"ConnectionRunnerThread");
            thread.start();
        }
        start.countDown();
        end.await();
        System.out.println("total invoke : "+(threadCount*count));
        System.out.println("got connection : "+got);
        System.out.println("not got connection : "+notgot);

    }

    private static class ConnectionRunner implements Runnable{
        int count;
        //计数器，测试看有多少次获得了，有多少次没有获得
        AtomicInteger got;
        AtomicInteger notgot;

        public ConnectionRunner(int count, AtomicInteger got, AtomicInteger notgot) {
            this.count = count;
            this.got = got;
            this.notgot = notgot;
        }

        @Override
        public void run() {
            try{
                start.await();
            }catch (Exception ignored){}
            while (count>0){
                try{
                    //超时机制，如果1秒内没有获得，将会返回null
                    Connection connection = pool.fetchConnection(1000);
                    if(connection!=null){
                        try{
                            connection.createStatement();
                            //占用100毫秒
                            connection.commit();
                        }finally {
                            pool.releaseConnection(connection);
                            got.incrementAndGet();
                        }
                    }else{
                        notgot.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    count--;
                }
            }
            end.countDown();
        }
    }
}

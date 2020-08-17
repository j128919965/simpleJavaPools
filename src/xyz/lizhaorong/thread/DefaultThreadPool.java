package xyz.lizhaorong.thread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class DefaultThreadPool<Job extends Runnable> implements ThreadPool<Job> {
    //线程池最大线程数
    private static final int MAX_WORKER_NUMBERS = 10;
    //默认线程数
    private static final int DEFAULT_WORKER_NUMBERS = 5;
    //线程池最小数量
    private static final int MIN_WORKER_NUMBERS = 1;
    //工作列表，将会往里插入工作
    private final LinkedList<Job> jobs = new LinkedList<>();
    //工作者列表，用于承担job的消费
    private final List<Worker> workers = Collections.synchronizedList(new ArrayList<>());
    //工作者线程的数量
    private int workerNum = DEFAULT_WORKER_NUMBERS;
    //线程编号生成
    private AtomicLong threadNum = new AtomicLong();

    public DefaultThreadPool(){
        initializeWorkers(DEFAULT_WORKER_NUMBERS);
    }

    public DefaultThreadPool(int num){
        workerNum = num>MAX_WORKER_NUMBERS?MAX_WORKER_NUMBERS: Math.max(num, MIN_WORKER_NUMBERS);
        initializeWorkers(workerNum);
    }

    private void initializeWorkers(int num){
        for(int i = 0; i < num; i++){
            Worker worker = new Worker();
            workers.add(worker);
            Thread thread = new Thread(worker,"ThreadPool-Worker-"+threadNum.incrementAndGet());
            thread.start();
        }
    }

    @Override
    public void excute(Job job) {
        if(job!=null){
            //添加一个工作，然后进行通知
            synchronized (jobs){
                jobs.addLast(job);
                jobs.notify();
            }
        }
    }

    @Override
    public void shutdown() {
        for (Worker worker : workers) {
            worker.shutdown();
        }
    }

    @Override
    public void addWorkers(int num) {
        synchronized (jobs){
            //防止超出最大值
            if(num + this.workerNum> MAX_WORKER_NUMBERS){
                num = MAX_WORKER_NUMBERS - this.workerNum;
            }
            initializeWorkers(num);
        }
    }

    @Override
    public void removeWorkers(int num) {
        synchronized (jobs){
            if(num>=this.workerNum){
                throw new IllegalArgumentException("beyond workerNum");
            }
            int count = 0;
            while (count<num){
                Worker worker = workers.get(count);
                // 先从workers中移除这个worker，如果成功的话，把这个线程关闭。
                // 该线程运行完当前任务后，就不再接收新的任务了
                // 该worker线程也随之结束
                if(workers.remove(worker)){
                    worker.shutdown();
                    count++;
                }
            }
            this.workerNum -= count;
        }
    }

    @Override
    public int getJobSize() {
        return jobs.size();
    }


    /**
     * 工作者类，用于承载任务的消费
     * 持续不断地消费Job
     * 发生InterruptedException的时候，关闭该线程
     */
    private class Worker implements Runnable{

        private volatile boolean running = true;
        public void run(){
            while (running){
                Job job;
                synchronized (jobs){
                    while (jobs.isEmpty()){
                        try {
                            jobs.wait();
                        }catch (InterruptedException e){
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    job = jobs.removeFirst();
                }
                if(job!=null){
                    try {
                        job.run();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }

        public void shutdown(){
            running = false;
        }

    }

}

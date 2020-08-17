package xyz.lizhaorong.connection;

import java.sql.Connection;
import java.util.LinkedList;

public class ConnectionPool {

    private final LinkedList<Connection>  pool = new LinkedList<>();

    public ConnectionPool(int size){
        if(size>0){
            for(int i=0;i<size;i++){
                pool.addLast(ConnectionDriver.createConnection());
            }
        }
    }

    public void releaseConnection(Connection connection){
        if(connection!=null){
            synchronized (pool){
                //归还连接后需要通知其他线程，这样其他线程才能感受到连接池中已经多了一个可用连接
                pool.addLast(connection);
                pool.notifyAll();
            }
        }
    }

    //主要是超时机制
    public Connection fetchConnection(long mills) throws InterruptedException{
        synchronized (pool){
            // 可以超时，不规定超时时间
            if (mills<=0){
                while (pool.isEmpty()){
                    pool.wait();
                }
                return pool.removeFirst();
            } else {
                long future = System.currentTimeMillis() + mills;
                long remaining = mills;
                while (pool.isEmpty() && remaining>0){
                    pool.wait(remaining);
                    remaining = future - System.currentTimeMillis();
                }
                Connection result = null;
                if(!pool.isEmpty()){
                    result = pool.removeFirst();
                }
                return result;
            }
        }
    }

}

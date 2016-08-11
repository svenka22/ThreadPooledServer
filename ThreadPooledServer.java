import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


class WorkerThread implements Runnable{

    public Socket socket = null;
    public int count = 0;

    public WorkerThread(Socket socket, int count) {
        this.socket = socket;
        this.count   = count;
    }

    public void run() {
        try {
            InputStream input  = socket.getInputStream();
            OutputStream output = socket.getOutputStream();
            long time = System.currentTimeMillis();
            output.write(("HTTP/1.1 200 OK\n\n Calling WorkerThread  " + this.count + " - " + time + "").getBytes());
            output.close();
            input.close();
            System.out.println("Request processed: " + time);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

public class ThreadPooledServer implements Runnable{

    public int serverPort = 0;
    public ServerSocket serverSocket = null;
    public boolean isStopped = false;
    public Thread runningThread= null;
    public static int count = 0;

    public ExecutorService threadPool = Executors.newFixedThreadPool(5);

    public ThreadPooledServer(int port){
        this.serverPort = port;
    }

    public void run(){
        synchronized(this){
            this.runningThread = Thread.currentThread();
        }
        openServerSocket();
        while(! isStopped()){
            Socket socket = null;
            try {
                socket = this.serverSocket.accept();
            } catch (IOException e) {
                if(isStopped()) {
                    System.out.println("Server Stopped.") ;
                    break;
                }
                throw new RuntimeException("Error accepting connection", e);
            }
            this.threadPool.execute(new WorkerThread(socket, count++));
        }
    }


    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop(){
        this.isStopped = true;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }

    private void openServerSocket() {
        count++;
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args){
        ThreadPooledServer server = new ThreadPooledServer(5000);
        new Thread(server).start();
    }
}
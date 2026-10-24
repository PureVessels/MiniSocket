package mini.service.socket.utils.socket;


public abstract class SocketThread extends Thread {

    public SocketThread(){

    }

    public SocketThread(String threadName) {
        super(threadName);
    }

    public abstract void shutdown();
}

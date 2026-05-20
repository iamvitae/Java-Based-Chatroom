package server;

public class Dispatcher implements Runnable {
    private final SharedBoundedBuffer sharedBuffer;

    public Dispatcher(SharedBoundedBuffer buffer) {
        this.sharedBuffer = buffer;
    }

    @Override
    public void run() {
        try {
            while (true) {
                String request = sharedBuffer.remove();
                
                Thread worker = new Thread(new Worker(request));
                worker.start();
            }
        } catch (InterruptedException e) {
            ServerUI.logMessage("Dispatcher interrupted.");
            Thread.currentThread().interrupt();
        }
    }
}
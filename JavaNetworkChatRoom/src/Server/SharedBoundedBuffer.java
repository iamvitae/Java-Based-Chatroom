package server;

import java.util.LinkedList;
import java.util.Queue;

public class SharedBoundedBuffer {
    private final Queue<String> buffer = new LinkedList<>();
    private final int capacity = 50; 

    public synchronized void insert(String request) throws InterruptedException {
        while (buffer.size() == capacity) {
            wait(); 
        }
        buffer.add(request);
        notifyAll(); 
    }

    
    public synchronized String remove() throws InterruptedException {
        while (buffer.isEmpty()) {
            wait(); 
        }
        String request = buffer.poll();
        notifyAll(); 
        return request;
    }
}
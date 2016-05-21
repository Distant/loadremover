package philoats.loadremover.analysis;

import java.util.ArrayList;
import java.util.List;

class FrameBuffer<T> {
    private List<T> buffer;
    private int bufferOffset = 0;
    private int bufferCapacity;

    FrameBuffer(int capacity) {
        buffer = new ArrayList<>();
        bufferCapacity = capacity;
    }

    T get(int i) {
        return buffer.get(i - bufferOffset);
    }

    T getRelative(int i) {
        return buffer.get(i);
    }

    void add(T t) {
        buffer.add(t);
        if (buffer.size() > bufferCapacity) {
            buffer.remove(0);
            bufferOffset++;
        }
    }

    int getOffset(){
        return bufferOffset;
    }
}
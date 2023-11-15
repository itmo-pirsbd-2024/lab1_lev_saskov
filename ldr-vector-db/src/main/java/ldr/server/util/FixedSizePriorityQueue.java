package ldr.server.util;

import java.util.PriorityQueue;

public class FixedSizePriorityQueue<T> extends PriorityQueue<T> {
    private final int maxSize;

    public FixedSizePriorityQueue(int maxSize) {
        // In add we have a moment, when size od queue is bigger then one size for one.
        super(maxSize + 1);
        this.maxSize = maxSize;
    }

    @Override
    public boolean add(T el) {
        boolean added = super.add(el);
        if (size() > maxSize) {
            T toDel = poll();
            return !el.equals(toDel);
        }

        return added;
    }
}

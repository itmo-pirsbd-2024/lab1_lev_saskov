package ldr.server.util;

import java.util.Comparator;
import java.util.PriorityQueue;

public class FixedSizePriorityQueue<T> extends PriorityQueue<T> {
    private final int maxSize;

    public FixedSizePriorityQueue(int maxSize, Comparator<? super T> comparator) {
        super(maxSize + 1, comparator);
        this.maxSize = maxSize;
    }

    public FixedSizePriorityQueue(int maxSize) {
        // In add we have a moment, when size od queue is bigger then one size for one.
        this(maxSize + 1, null);
    }

    @Override
    public boolean add(T el) {
        boolean added = super.add(el);
        if (size() > maxSize) {
            // Delete min or max accord to order.
            T toDel = poll();
            return !el.equals(toDel);
        }

        return added;
    }
}

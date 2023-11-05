package ldr.server.storage.drive;

import java.util.Iterator;
import java.util.NoSuchElementException;

import ldr.client.domen.Embedding;
import ldr.server.util.PeekIterator;
import ldr.server.storage.StorageManager;

public class GravedMergedIterator implements Iterator<Embedding> {
    private final PeekIterator out;
    private final PeekIterator in;
    private Embedding next;

    /**
     * Priority for out, out can have graves.
     */
    GravedMergedIterator(PeekIterator out, PeekIterator in) {
        this.out = out;
        this.in = in;
    }

    @Override
    public boolean hasNext() {
        if (next == null) {
            next = withSkippedGraves();
        }

        return next != null;
    }

    @Override
    public Embedding next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        var ans = next;
        next = null;
        return ans;
    }

    private Embedding withSkippedGraves() {
        Embedding result = null;
        while ((out.hasNext() || in.hasNext()) && StorageManager.isGrave(result = getNext())) {
            // Действие происходит в проверке условия. Таким образом мы скипаем могилы.
        }

        return result == null || StorageManager.isGrave(result) ? null : result;
    }

    private Embedding getNext() {
        if (!out.hasNext()) {
            return in.next();
        }

        if (!in.hasNext()) {
            return out.next();
        }

        if (out.peek().id() == in.peek().id()) {
            in.next();
            return out.next();
        }


        return out.peek().id() < in.peek().id() ? out.next() : in.next();
    }
}

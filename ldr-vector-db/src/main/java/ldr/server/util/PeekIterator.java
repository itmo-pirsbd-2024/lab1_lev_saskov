package ldr.server.util;

import java.util.Iterator;

import ldr.client.domen.Embedding;

public class PeekIterator implements Iterator<Embedding> {
    private final Iterator<Embedding> delegate;
    private Embedding current;

    public PeekIterator(Iterator<Embedding> delegate) {
        this.delegate = delegate;
    }

    public Embedding peek() {
        if (current == null && delegate.hasNext()) {
            current = delegate.next();
        }
        return current;
    }

    @Override
    public boolean hasNext() {
        return current != null || delegate.hasNext();
    }

    @Override
    public Embedding next() {
        Embedding peek = peek();
        current = null;
        return peek;
    }
}

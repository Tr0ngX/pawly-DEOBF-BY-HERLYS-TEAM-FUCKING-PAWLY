package com.google.common.collect;
import java.util.*;

public class ImmutableList<E> extends ArrayList<E> {
    @SafeVarargs
    public static <E> ImmutableList<E> of(E... elements) {
        ImmutableList<E> list = new ImmutableList<>();
        Collections.addAll(list, elements);
        return list;
    }
    public static <E> Builder<E> builder() { return new Builder<>(); }
    public static class Builder<E> {
        private final ImmutableList<E> list = new ImmutableList<>();
        public Builder<E> add(E element) { list.add(element); return this; }
        public Builder<E> addAll(Iterable<? extends E> elements) {
            for (E e : elements) list.add(e);
            return this;
        }
        public ImmutableList<E> build() { return list; }
    }
}

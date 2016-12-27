package no.unioner;

public final class UnionTypeDelegate<T> {

    public static final <T> UnionTypeDelegate<T> impl(@SuppressWarnings("unused") Class<T> delegateType, T target) {
        return new UnionTypeDelegate<>(target);
    }


    final T delegate;

    private UnionTypeDelegate(T delegate) {
        this.delegate = delegate;
    }
}

package no.unioner;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;

public class Unioner {

    public static <U> Builder<U> forType(Class<U> unionType) {
        return new Builder<>(unionType);
    }

    public static final class Builder<U> {
        final Class<U> unionType;
        final Stream.Builder<UnionTypeDelegate<? super U>> delegates = Stream.builder();

        private Builder(Class<U> unionType) {
            this.unionType = unionType;
        }

        public Builder<U> delegateTo(UnionTypeDelegate<? super U> delegate) {
            delegates.add(delegate);
            return this;
        }

        public U createInstance() {
            return Unioner.createInstance(unionType, delegates.build());
        }

    }




    private static <U> U createInstance(Class<U> unionType, Stream<UnionTypeDelegate<? super U>> delegates) {
        if (!unionType.isInterface()) {
            throw new IllegalArgumentException(unionType + " must be an interface.");
        }

        Set<DelegateCandidate> delegateCandidates = delegates.map(DelegateCandidate::new).collect(toCollection(LinkedHashSet::new));
        InvocationHandler delegationHandler = (Object proxy, Method method, Object[] args) -> delegateCandidates.stream().sequential()
                  .flatMap(d -> d.resolveApplicableMethod(method, args))
                  .findFirst()
                  .map(d -> d.invoke(args))
                  .orElseThrow(() -> new UnsupportedOperationException("No delegate was found for " + method + " for arguments " + Arrays.toString(args)));

        @SuppressWarnings("unchecked")
        U instance = (U) Proxy.newProxyInstance(Unioner.class.getClassLoader(), new Class[]{unionType}, delegationHandler);
        return instance;
    }

    static final class DelegateCandidate {
        private final Set<BoundMethod> publicMethods;

        public DelegateCandidate(UnionTypeDelegate<?> delegate) {
            this.publicMethods = Stream.of(delegate.delegate.getClass().getMethods()).map(m -> new BoundMethod(delegate, m)).collect(toSet());
        }

        public Stream<BoundMethod> resolveApplicableMethod(Method method, Object[] args) {
            Class<?>[] paramTypes = Stream.of(args).map(Object::getClass).toArray(Class[]::new);
            for (BoundMethod delegateMethod : publicMethods) {
                if (delegateMethod.nameAndSignatureCompatibleWith(method, paramTypes)) {
                    return Stream.of(delegateMethod);
                }
            }
            return Stream.empty();
        }
    }

    static final Map<Class<?>, Class<?>> boxedPrimitives = new HashMap<>(); static {
        boxedPrimitives.put(boolean.class, Boolean.class);
        boxedPrimitives.put(byte.class, Byte.class);
        boxedPrimitives.put(short.class, Short.class);
        boxedPrimitives.put(int.class, Integer.class);
        boxedPrimitives.put(char.class, Character.class);
        boxedPrimitives.put(long.class, Long.class);
        boxedPrimitives.put(float.class, Float.class);
        boxedPrimitives.put(double.class, Double.class);
    }

    static final class BoundMethod {
        private final UnionTypeDelegate<?> target;
        private final Method method;

        BoundMethod(UnionTypeDelegate<?> target, Method method) {
            this.target = target;
            this.method = method;
        }

        boolean nameAndSignatureCompatibleWith(Method method, Class<?>[] paramTypes) {
            return this.method.getName().equals(method.getName()) && compatibleParams(paramTypes);
        }

        boolean compatibleParams(Class<?>[] paramTypes) {
            if (method.getParameterTypes().length == paramTypes.length) {
                for (int i = 0; i < paramTypes.length; i++) {
                    Class<?> paramType = method.getParameterTypes()[i];
                    if ((paramType.isPrimitive() ? boxedPrimitives.get(paramType) : paramType) != paramTypes[i]) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        public Object invoke(Object[] args) {
            try {
                return method.invoke(target.delegate, args);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }
}

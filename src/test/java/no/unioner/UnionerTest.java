package no.unioner;

import org.junit.Test;

import static no.unioner.UnionTypeDelegate.impl;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class UnionerTest {

    @FunctionalInterface
    interface A {
        String convert(Integer i);
    }

    @FunctionalInterface
    interface B {
        String translate(String s);
    }

    @FunctionalInterface
    interface C {
        int proc(int s);
    }


    interface UnionType extends A, B, C {}

    @Test
    public void unionTypeDelegatesToVariousSuperInterfaceInstances() {
        UnionType unionInstance = Unioner.forType(UnionType.class)
                .delegateTo(impl(A.class, String::valueOf)).delegateTo(impl(B.class, String::toLowerCase)).delegateTo(impl(C.class, i -> i + 1))
                .createInstance();
        assertThat(unionInstance.convert(42), is("42"));
        assertThat(unionInstance.translate("HALLOI"), is("halloi"));
        assertThat(unionInstance.proc(42), is(43));
    }
}

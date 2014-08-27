package test;

public class NestedEnumArgument {
    public enum E {
        FIRST
    }

    @interface Anno {
        E value();
    }

    @Anno(E.FIRST)
    void foo() {}
}

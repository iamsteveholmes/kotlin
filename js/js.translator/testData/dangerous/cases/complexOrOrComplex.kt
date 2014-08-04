package foo

var sideEffect: Int = 0;

fun id(value: Boolean): Boolean = value

fun box(): String {

    assertEquals(true, when { else -> { ++sideEffect; true }} || when { else -> { ++sideEffect; true }}, "true || true")
    assertEquals(1, sideEffect, "true || true side effect")

    assertEquals(true, when { else -> { ++sideEffect; false }} || when { else -> { ++sideEffect; true }}, "true || false")
    assertEquals(3, sideEffect, "true || false side effect")

    assertEquals(true, when { else -> { ++sideEffect; true }} || when { else -> { ++sideEffect; false }}, "false || true")
    assertEquals(4, sideEffect, "false || true side effect")

    assertEquals(false, when { else -> { ++sideEffect; false }} || when { else -> { ++sideEffect; false }}, "false || false")
    assertEquals(6, sideEffect, "false || false side effect")

    return "OK"
}

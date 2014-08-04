package foo

var sideEffect: Int = 0;

fun id(value: Boolean): Boolean = value

fun box(): String {

    assertEquals(true, id(true) || when { else -> { ++sideEffect; true }}, "true || true")
    assertEquals(0, sideEffect, "true || true side effect")

    assertEquals(true, id(true) || when { else -> { ++sideEffect; false }}, "true || false")
    assertEquals(0, sideEffect, "true || false side effect")

    assertEquals(true, id(false) || when { else -> { ++sideEffect; true }}, "false || true")
    assertEquals(1, sideEffect, "false || true side effect")

    assertEquals(false, id(false) || when { else -> { ++sideEffect; false }}, "false || false")
    assertEquals(2, sideEffect, "false || false side effect")

    return "OK"
}

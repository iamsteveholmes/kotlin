package foo

var sideEffect: Int = 0;

fun id(value: Boolean): Boolean = value

fun box(): String {

    assertEquals(true, when { else -> { ++sideEffect; true }} && id(true), "true && true")
    assertEquals(1, sideEffect, "true && true side effect")

    assertEquals(false, when { else -> { ++sideEffect; false }} && id(true), "true && false")
    assertEquals(2, sideEffect, "true && false side effect")

    assertEquals(false, when { else -> { ++sideEffect; true }} && id(false), "false && true")
    assertEquals(3, sideEffect, "false && true side effect")

    assertEquals(false, when { else -> { ++sideEffect; false }} && id(false), "false && false")
    assertEquals(4, sideEffect, "false && false side effect")

    return "OK"
}

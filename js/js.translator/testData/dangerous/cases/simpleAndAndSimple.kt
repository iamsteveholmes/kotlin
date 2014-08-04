package foo

fun id(value: Boolean): Boolean = value

fun box(): String {

    assertEquals(true, id(true) && id(true), "true && true")
    assertEquals(false, id(true) && id(false), "true && false")
    assertEquals(false, id(false) && id(true), "false && true")
    assertEquals(false, id(false) && id(false), "false && false")

    assertEquals(true, id(true) || id(true), "true || true")
    assertEquals(true, id(true) || id(false), "true || false")
    assertEquals(true, id(false) || id(true), "false || true")
    assertEquals(false, id(false) || id(false), "false || false")

    return "OK"
}

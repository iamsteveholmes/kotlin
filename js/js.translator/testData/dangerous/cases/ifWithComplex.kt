package foo

var sideEffect: Int = 0;

fun id(value: Boolean): Boolean = value

fun box(): String {

    assertEquals(if (id(true)) when { else -> { ++sideEffect; 10 }} else 20, 10)
    assertEquals(1, sideEffect)

    assertEquals(if (id(false)) when { else -> { ++sideEffect; 10 }} else 20, 20)
    assertEquals(1, sideEffect)

    assertEquals(if (id(true)) 100 else when { else -> { ++sideEffect; 200 }}, 100)
    assertEquals(1, sideEffect)

    assertEquals(if (id(false)) 100 else when { else -> { ++sideEffect; 200 }}, 200)
    assertEquals(2, sideEffect)

    assertEquals(if (id(true)) when { else -> { ++sideEffect; 1000 }} else when { else -> { ++sideEffect; 2000 }}, 1000)
    assertEquals(3, sideEffect)

    assertEquals(if (id(false)) when { else -> { ++sideEffect; 1000 }} else when { else -> { ++sideEffect; 2000 }}, 2000)
    assertEquals(4, sideEffect)

    return "OK"
}

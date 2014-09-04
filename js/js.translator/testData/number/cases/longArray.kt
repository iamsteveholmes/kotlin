package foo

fun box(): String {

    val arr = LongArray(2)

    val expected: Long = 0
    assertEquals(arr.size, 2)
    assertEquals(expected, arr[0])
    assertEquals(expected, arr[1])

    val arr1 = longArray(1,2,3)
    assertEquals(1L, arr1[0])
    assertEquals(2L, arr1[1])
    assertEquals(3L, arr1[2])

    return "OK"
}

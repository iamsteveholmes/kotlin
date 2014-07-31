import test.*

class Holder {
    var value: String = ""
}

fun test0(h: Holder, throwEx1: Boolean, throwEx2: Boolean, throwEx3: Boolean = false, throwEx4: Boolean = false): String {
    val localResult = doCall (
            {
                h.value += "OK_NON_LOCAL"
                if (throwEx1) {
                    throw Exception1("1")
                }
                if (throwEx2) {
                    throw Exception2("1")
                }
                return "OK_NON_LOCAL"
            },
            {
                h.value += ", OK_EXCEPTION1"
                if (throwEx3) {
                    throw Exception1("3_1")
                }
                if (throwEx4) {
                    throw Exception2("4_1")
                }
                return "OK_EXCEPTION1"
            },
            {
                h.value += ", OK_EXCEPTION2"
                if (throwEx3) {
                    throw Exception1("3_2")
                }
                if (throwEx4) {
                    throw Exception2("4_2")
                }
                return "OK_EXCEPTION2"
            },
            {
                h.value += ", OK_FINALLY1"
                "OK_FINALLY1"
            },
            {
                h.value += ", OK_EXCEPTION3"
                return "OK_EXCEPTION3"
            },
            {
                h.value += ", OK_EXCEPTION4"
                return "OK_EXCEPTION4"
            },
            {
                h.value += ", OK_FINALLY2"
                "OK_FINALLY2"
            })

    return localResult;

    return "FAIL";
}

/*
fun test01(h: Holder): String {
    val localResult = doCall (
            {
                h.value += "OK_NON_LOCAL"
                throw Exception1("1")
                return "OK_NON_LOCAL"
            },
            {
                h.value += ", OK_EXCEPTION1"
                return "OK_EXCEPTION1"
            },
            {
                h.value += ", OK_EXCEPTION2"
                return "OK_EXCEPTION2"
            },
            {
                h.value += ", OK_FINALLY"
                "OK_FINALLY"
            })

    return localResult;
}

fun test02(h: Holder): String {
    val localResult = doCall (
            {
                h.value += "OK_NON_LOCAL"
                throw Exception2("1")
                return "OK_NON_LOCAL"
            },
            {
                h.value += ", OK_EXCEPTION1"
                return "OK_EXCEPTION1"
            },
            {
                h.value += ", OK_EXCEPTION2"
                return "OK_EXCEPTION2"
            },
            {
                h.value += ", OK_FINALLY"
                "OK_FINALLY"
            })

    return localResult;
}

fun test1(h: Holder): String {
    try {
        val localResult = doCall (
                {
                    h.value += "OK_LOCAL"
                    throw Exception1("FAIL")
                    "OK_LOCAL"
                },
                {
                    h.value += ", OK_EXCEPTION1"
                    return "OK_EXCEPTION1"
                },
                {
                    h.value += ", OK_EXCEPTION2"
                    return "OK_EXCEPTION2"
                },
                {
                    h.value += ", OK_FINALLY"
                    throw java.lang.RuntimeException("FINALLY")
                    "OK_FINALLY"
                }, "Fail")
    }
    catch (e: RuntimeException) {
        if (e.getMessage() != "FINALLY") {
            return "FAIL in exception: " + e.getMessage()
        }
        else {
            return "CATCHED_EXCEPTION"
        }
    }

    return "FAIL";
}

fun test2(h: Holder): String {
    try {
        val localResult = doCall (
                {
                    h.value += "OK_LOCAL"
                    throw Exception1("1")
                    "OK_LOCAL"
                },
                {
                    h.value += ", OK_EXCEPTION1"
                    throw Exception2("2")
                    "OK_EXCEPTION"
                },
                {
                    h.value += ", OK_EXCEPTION2"
                    "OK_EXCEPTION2"
                },
                {
                    h.value += ", OK_FINALLY"
                    "OK_FINALLY"
                })
        return localResult;
    }
    catch (e: Exception2) {
        return "CATCHED_EXCEPTION"
    }

    return "Fail";
}

fun test3(h: Holder): String {
    try {
        val localResult = doCall (
                {
                    h.value += "OK_LOCAL"
                    throw Exception2("FAIL")
                    "OK_LOCAL"
                },
                {
                    h.value += ", OK_EXCEPTION1"
                    return "OK_EXCEPTION1"
                },
                {
                    h.value += ", OK_EXCEPTION2"
                    return "OK_EXCEPTION2"
                },
                {
                    h.value += ", OK_FINALLY"
                    throw java.lang.RuntimeException("FINALLY")
                    "OK_FINALLY"
                }, "Fail")
    }
    catch (e: RuntimeException) {
        if (e.getMessage() != "FINALLY") {
            return "FAIL in exception: " + e.getMessage()
        }
        else {
            return "CATCHED_EXCEPTION"
        }
    }

    return "FAIL";
}

fun test4(h: Holder): String {
    try {
        val localResult = doCall (
                {
                    h.value += "OK_LOCAL"
                    throw Exception2("1")
                    "OK_LOCAL"
                },
                {
                    h.value += ", OK_EXCEPTION1"
                    return "OK_EXCEPTION"
                },
                {
                    h.value += ", OK_EXCEPTION2"
                    throw Exception1("1")
                    "OK_EXCEPTION2"
                },
                {
                    h.value += ", OK_FINALLY"
                    "OK_FINALLY"
                })
        return localResult;
    }
    catch (e: Exception1) {
        return "CATCHED_EXCEPTION"
    }

    return "Fail";
}


fun test5(h: Holder): String {
    val localResult = doCall (
            {
                h.value += "OK_LOCAL"
                throw Exception2("FAIL")
                "OK_LOCAL"
            },
            {
                h.value += ", OK_EXCEPTION"
                throw java.lang.RuntimeException("FAIL_EX")
                "OK_EXCEPTION"
            },
            {
                h.value += ", OK_EXCEPTION2"
                return "OK_EXCEPTION2"
            },
            {
                h.value += ", OK_FINALLY"
                "OK_FINALLY"
            })


    return localResult;
}
*/

fun box(): String {
    var h = Holder()
    var test0 = test0(h, false, false)
    if (test0 != "OK_NON_LOCAL" || h.value != "OK_NON_LOCAL, OK_FINALLY1, OK_FINALLY2") return "test0_1: ${test0}, holder: ${h.value}"

    h = Holder()
    test0 = test0(h, true, false)
    if (test0 != "OK_EXCEPTION1" || h.value != "OK_NON_LOCAL, OK_EXCEPTION1, OK_FINALLY1, OK_FINALLY2") return "test0_2: ${test0}, holder: ${h.value}"

    h = Holder()
    test0 = test0(h, false, true)
    if (test0 != "OK_EXCEPTION2" || h.value != "OK_NON_LOCAL, OK_EXCEPTION2, OK_FINALLY1, OK_FINALLY2") return "test0_3: ${test0}, holder: ${h.value}"

    h = Holder()
    test0 = test0(h, true, false, true, false)
    if (test0 != "OK_EXCEPTION3" || h.value != "OK_NON_LOCAL, OK_EXCEPTION1, OK_FINALLY1, OK_EXCEPTION3, OK_FINALLY2") return "test0_4: ${test0}, holder: ${h.value}"

    h = Holder()
    test0 = test0(h, true, false, false, true)
    if (test0 != "OK_EXCEPTION4" || h.value != "OK_NON_LOCAL, OK_EXCEPTION1, OK_FINALLY1, OK_EXCEPTION4, OK_FINALLY2") return "test0_5: ${test0}, holder: ${h.value}"

    h = Holder()
    test0 = test0(h, false, true, true, false)
    if (test0 != "OK_EXCEPTION3" || h.value != "OK_NON_LOCAL, OK_EXCEPTION2, OK_FINALLY1, OK_EXCEPTION3, OK_FINALLY2") return "test0_6: ${test0}, holder: ${h.value}"

    h = Holder()
    test0 = test0(h, false, true, false, true)
    if (test0 != "OK_EXCEPTION4" || h.value != "OK_NON_LOCAL, OK_EXCEPTION2, OK_FINALLY1, OK_EXCEPTION4, OK_FINALLY2") return "test0_7: ${test0}, holder: ${h.value}"

    return "OK"
}

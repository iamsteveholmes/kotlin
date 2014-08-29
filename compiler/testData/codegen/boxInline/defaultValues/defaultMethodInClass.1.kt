import test.*

fun box(): String {
    if (Z().run() != null) return "fail 1"

    if (Z().run("OK") != "OK") return "fail 2"

    return "OK"
}
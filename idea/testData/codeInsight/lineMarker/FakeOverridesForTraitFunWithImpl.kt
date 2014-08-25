trait <lineMarker descr="*"></lineMarker>A {
    fun <lineMarker descr="<html><body>Is implemented in <br>&nbsp;&nbsp;&nbsp;&nbsp;C</body></html>"></lineMarker>foo(): String = "A"
}

abstract class <lineMarker descr="*"></lineMarker>B: A

class C: B() {
    override fun <lineMarker descr="*"></lineMarker>foo() = "C"
}

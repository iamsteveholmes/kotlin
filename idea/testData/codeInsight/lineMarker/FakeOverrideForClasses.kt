trait S<T> {
    fun foo(t: T): T = t
}

open class S1: S<String>

class S2: S1() {
    override fun foo(t: String): String {
        return super<S1>.foo(t)
    }
}
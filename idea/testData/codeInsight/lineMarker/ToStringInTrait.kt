public trait Foo {
    override fun <lineMarker descr="Overrides function in 'Any'"></lineMarker>toString() = "str"
}

/*
Overrides function in 'Any'
Any.kt
    public open fun <1>toString(): String
*/
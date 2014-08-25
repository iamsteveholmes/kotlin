trait <lineMarker descr="*"></lineMarker>SkipSupport {
    fun <lineMarker descr="*"></lineMarker>skip(why: String)
    fun <lineMarker descr="*"></lineMarker>skip()
}

public trait <lineMarker descr="*"></lineMarker>SkipSupportWithDefaults : SkipSupport {
    override fun <lineMarker descr="*"><lineMarker descr="Implements function in 'SkipSupport'"></lineMarker></lineMarker>skip(why: String) {}
    override fun <lineMarker descr="*"><lineMarker descr="Implements function in 'SkipSupport'"></lineMarker></lineMarker>skip() {
        skip("not given")
    }
}

open class SkipSupportImpl: SkipSupportWithDefaults {
    override fun <lineMarker descr="Overrides function in 'SkipSupportWithDefaults'"></lineMarker>skip(why: String) = throw RuntimeException(why)
}

// KT-4428 Incorrect override icon shown for overloaded methods
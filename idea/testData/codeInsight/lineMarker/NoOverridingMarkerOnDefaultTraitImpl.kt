trait <lineMarker descr="*"></lineMarker>SkipSupport {
    fun <lineMarker descr="*"></lineMarker>skip()
}

public trait <lineMarker descr="*"></lineMarker>SkipSupportWithDefaults : SkipSupport {
    override fun <lineMarker descr="Implements function in 'SkipSupport'"></lineMarker>skip() {}
}

open class SkipSupportImpl : SkipSupportWithDefaults
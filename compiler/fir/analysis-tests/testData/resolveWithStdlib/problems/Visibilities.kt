// FILE: Visibility.kt

abstract class Visibility protected constructor(
    val name: String,
    val isPublicAPI: Boolean
) {
    open val internalDisplayName: String
        get() = name

    open val externalDisplayName: String
        get() = internalDisplayName

    abstract fun mustCheckInImports(): Boolean

    open fun compareTo(visibility: Visibility): Int? {
        return Visibilities.compareLocal(this, visibility)
    }

    final override fun toString() = internalDisplayName

    open fun normalize(): Visibility = this
}

// FILE: Visibilities.kt

object Visibilities {
    object Private : Visibility("private", isPublicAPI = false) {
        override fun mustCheckInImports(): Boolean = true
    }

    object PrivateToThis : Visibility("private_to_this", isPublicAPI = false) {
        override val internalDisplayName: String
            get() = "private/*private to this*/"

        override fun mustCheckInImports(): Boolean = true
    }

    object Protected : Visibility("protected", isPublicAPI = true) {
        override fun mustCheckInImports(): Boolean = false
    }

    object Internal : Visibility("internal", isPublicAPI = false) {
        override fun mustCheckInImports(): Boolean = true
    }

    object Public : Visibility("public", isPublicAPI = true) {
        override fun mustCheckInImports(): Boolean = false
    }

    object Local : Visibility("local", isPublicAPI = false) {
        override fun mustCheckInImports(): Boolean = true
    }

    object Inherited : Visibility("inherited", isPublicAPI = false) {
        override fun mustCheckInImports(): Boolean {
            throw IllegalStateException("This method shouldn't be invoked for INHERITED visibility")
        }
    }

    object InvisibleFake : Visibility("invisible_fake", isPublicAPI = false) {
        override fun mustCheckInImports(): Boolean = true

        override val externalDisplayName: String
            get() = "invisible (private in a supertype)"
    }

    object Unknown : Visibility("unknown", isPublicAPI = false) {
        override fun mustCheckInImports(): Boolean {
            throw IllegalStateException("This method shouldn't be invoked for UNKNOWN visibility")
        }
    }

    private val ORDERED_VISIBILITIES: Map<Visibility, Int> = buildMap {
        put(PrivateToThis, 0)
        put(Private, 0);
        put(Internal, 1);
        put(Protected, 1);
        put(Public, 2);
    }

    fun compare(first: Visibility, second: Visibility): Int? {
        val result = first.compareTo(second)
        if (result != null) {
            return result
        }
        val oppositeResult = second.compareTo(first)
        return if (oppositeResult != null) {
            -oppositeResult
        } else null
    }

    internal fun compareLocal(first: Visibility, second: Visibility): Int? {
        if (first === second) return 0
        val firstIndex = ORDERED_VISIBILITIES[first]
        val secondIndex = ORDERED_VISIBILITIES[second]
        return if (firstIndex == null || secondIndex == null || firstIndex == secondIndex) {
            null
        } else firstIndex - secondIndex
    }

    fun isPrivate(visibility: Visibility): Boolean {
        return visibility === Private || visibility === PrivateToThis
    }

    val DEFAULT_VISIBILITY = Public
}

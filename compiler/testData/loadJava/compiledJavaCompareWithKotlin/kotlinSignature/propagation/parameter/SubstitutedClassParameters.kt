package test

public trait SubstitutedClassParameters: Object {

    public trait Super1<T>: Object {
        public fun foo(p0: T)
    }

    public trait Super2<E>: Object {
        public fun foo(p0: E)
    }

    public trait Sub: Super1<String>, Super2<String> {
        override fun foo(p0: String)
    }
}
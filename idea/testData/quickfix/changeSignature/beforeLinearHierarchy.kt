// "Add parameter to function 'f'" "true"
trait A

trait O {
    fun f(a: Int)
}

trait OO : O {
    override fun f(a: Int) {
    }
}

trait OOO : OO {
    override fun f(a: Int) {}
}

fun usage1(o: O) {
    o.f(1)
}

fun usage2(o: OO) {
    o.f(13, <caret>12)
}

fun usage3(o: OOO) {
    o.f(3)
}

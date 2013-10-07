import java.lang.annotation.*
import java.util.Arrays

[Retention(RetentionPolicy.RUNTIME)] annotation class ann

class Outer {
    inner class Inner(ann p: String) {
    }
}

fun box(): String {
    val annotations = Arrays.deepToString(Class.forName("Outer\$Inner")
                                                  .getConstructor(Class.forName("Outer"), Class.forName("java.lang.String"))
                                                  .getParameterAnnotations())
    if (annotations != "[[@jet.runtime.typeinfo.JetValueParameter(type=?, name=\$outer)], [@ann(), @jet.runtime.typeinfo.JetValueParameter(type=, name=p)]]")
        return annotations
    return "OK"
}
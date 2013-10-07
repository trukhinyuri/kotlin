import java.lang.annotation.*
import java.util.Arrays

[Retention(RetentionPolicy.RUNTIME)] annotation class ann

class C {
    fun String.f(ann p: String) {
    }
}

fun box(): String {
    val annotations = Arrays.deepToString(Class.forName("C")
                                                  .getMethod("f", Class.forName("java.lang.String"), Class.forName("java.lang.String"))
                                                  .getParameterAnnotations())
    if (annotations != "[[@jet.runtime.typeinfo.JetValueParameter(type=, name=\$receiver)], [@ann(), @jet.runtime.typeinfo.JetValueParameter(type=, name=p)]]")
        return annotations
    return "OK"
}
import java.lang.annotation.*
import java.util.Arrays

[Retention(RetentionPolicy.RUNTIME)] annotation class ann

enum class E(ann p: String) {
}

fun box(): String {
    val annotations = Arrays.deepToString(Class.forName("E")
                                              .getDeclaredConstructor(Class.forName("java.lang.String"), Integer.TYPE, Class.forName("java.lang.String"))
                                              .getParameterAnnotations())
    if (annotations != "[[@ann(), @jet.runtime.typeinfo.JetValueParameter(type=, name=p)]]") return annotations
    return "OK"
}
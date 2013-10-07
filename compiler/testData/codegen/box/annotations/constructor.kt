import java.lang.annotation.*
import java.util.Arrays

[Retention(RetentionPolicy.RUNTIME)] annotation class ann

class C(ann p: String) {
}

fun box(): String {
    val annotations = Arrays.toString(Class.forName("C").getConstructor(Class.forName("java.lang.String")).getParameterAnnotations()[0])
    if (annotations != "[@ann(), @jet.runtime.typeinfo.JetValueParameter(type=, name=p)]") return annotations
    return "OK"
}
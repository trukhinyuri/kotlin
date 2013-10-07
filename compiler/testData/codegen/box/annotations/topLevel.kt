import java.lang.annotation.*
import java.util.Arrays

[Retention(RetentionPolicy.RUNTIME)] annotation class ann

fun f(ann p: String) {
}

fun box(): String {
    val annotations = Arrays.toString(Class.forName("_DefaultPackage")
                                              .getMethod("f", Class.forName("java.lang.String"))
                                              .getParameterAnnotations()[0])
    if (annotations != "[@ann(), @jet.runtime.typeinfo.JetValueParameter(type=, name=p)]") return annotations
    return "OK"
}
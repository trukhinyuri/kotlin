package org.jetbrains.jet.lang.resolve.java.lazy.descriptors

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.resolve.java.JavaClassFinder
import org.jetbrains.jet.lang.resolve.lazy.storage.StorageManager
import org.jetbrains.jet.lang.resolve.name.Name
import java.util.Collections

public class LazyJavaPackageFragmentScope(
        containingDeclaration: NamespaceDescriptor, storageManager: StorageManager, finder: JavaClassFinder
) : LazyJavaMemberScope(containingDeclaration, storageManager, finder) {
    
    override fun getAllClassNames(): Collection<Name> {
        val fqName = DescriptorUtils.getFQName(getContainingDeclaration()).toSafe()
        val javaPackage = finder.findPackage(fqName)
        assert(javaPackage != null) { "Package not found:  $fqName" }
        return javaPackage!!.getClasses().map { c -> c.getName() }
    }

    override fun getAllPropertyNames() = Collections.emptyList<Name>()
    override fun getAllFunctionNames() = Collections.emptyList<Name>()
    
    override fun addExtraDescriptors(result: MutableCollection<in DeclarationDescriptor>) {
        throw UnsupportedOperationException()
    }

    override fun getClassifier(name: Name): ClassifierDescriptor {
        throw UnsupportedOperationException()
    }

    override fun getProperties(name: Name): MutableCollection<VariableDescriptor> {
        throw UnsupportedOperationException()
    }

    override fun getFunctions(name: Name): MutableCollection<FunctionDescriptor> {
        throw UnsupportedOperationException()
    }

    override fun getImplicitReceiversHierarchy(): MutableList<ReceiverParameterDescriptor> {
        throw UnsupportedOperationException()
    }

}

package org.jetbrains.jet.lang.resolve.java.lazy.descriptors

import org.jetbrains.annotations.Nullable
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.resolve.java.JavaClassFinder
import org.jetbrains.jet.lang.resolve.java.structure.JavaPackage
import org.jetbrains.jet.lang.resolve.lazy.storage.StorageManager
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.name.Name
import java.util.Collections

public class LazyJavaPackageFragmentScope(
        containingDeclaration: NamespaceDescriptor, storageManager: StorageManager, finder: JavaClassFinder
) : LazyJavaMemberScope(containingDeclaration, storageManager, finder) {
    
    protected override fun getAllClassNames(): Collection<Name> {
        val fqName = DescriptorUtils.getFQName(getContainingDeclaration()).toSafe()
        val javaPackage = finder.findPackage(fqName)
        assert(javaPackage != null) { "Package not found:  $fqName" }
        return null
    }
    protected override fun getAllPropertyNames(): Collection<Name> {
        return Collections.emptyList()
    }
    protected override fun getAllFunctionNames(): Collection<Name> {
        return Collections.emptyList()
    }
    protected override fun addExtraDescriptors(result: Collection<in DeclarationDescriptor>): Unit {
        throw UnsupportedOperationException()
    }
    public override fun getClassifier(name: Name): ClassifierDescriptor {
        throw UnsupportedOperationException()
    }
    public override fun getProperties(name: Name): Collection<valiableDescriptor> {
        throw UnsupportedOperationException()
    }
    public override fun getFunctions(name: Name): Collection<FunctionDescriptor> {
        throw UnsupportedOperationException()
    }
    public override fun getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> {
        throw UnsupportedOperationException()
    }


}

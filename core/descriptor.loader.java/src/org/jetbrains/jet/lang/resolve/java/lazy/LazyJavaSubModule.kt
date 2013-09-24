package org.jetbrains.jet.lang.resolve.java.lazy

import com.intellij.util.Function
import org.jetbrains.annotations.Nullable
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.LazyJavaPackageFragment
import org.jetbrains.jet.lang.resolve.lazy.storage.MemoizedFunctionToNullable
import org.jetbrains.jet.lang.resolve.lazy.storage.StorageManager
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.lazy.storage.StorageManager.ReferenceKind.STRONG

public open class LazyJavaSubModule(
        private val storageManager: StorageManager,
        private val module: ModuleDescriptor
) {
    public val packageFragments: MemoizedFunctionToNullable<FqName, NamespaceDescriptor>
            = storageManager.createMemoizedFunctionWithNullableValues(
                {fqName -> LazyJavaPackageFragment(module, fqName!!, LazyJavaPackageFragmentScope())},
                STRONG)
}
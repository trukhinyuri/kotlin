package org.jetbrains.jet.lang.resolve.java.lazy;

import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.LazyJavaPackageFragment;
import org.jetbrains.jet.lang.resolve.lazy.storage.MemoizedFunctionToNullable;
import org.jetbrains.jet.lang.resolve.lazy.storage.StorageManager;
import org.jetbrains.jet.lang.resolve.name.FqName;

import static org.jetbrains.jet.lang.resolve.lazy.storage.StorageManager.ReferenceKind.STRONG;

public class LazyJavaSubModule {
    private final StorageManager storageManager;

    private final ModuleDescriptor module;

    private final MemoizedFunctionToNullable<FqName, NamespaceDescriptor> packageFragments;

    public LazyJavaSubModule(
            StorageManager storageManager,
            ModuleDescriptor module
    ) {
        this.storageManager = storageManager;
        this.module = module;
        this.packageFragments = storageManager.createMemoizedFunctionWithNullableValues(new Function<FqName, NamespaceDescriptor>() {
            @Override
            public NamespaceDescriptor fun(FqName fqName) {
                return computePackageFragment(fqName);
            }
        }, STRONG);
    }

    @Nullable
    public NamespaceDescriptor getPackageFragment(@NotNull FqName fqName) {
        return packageFragments.fun(fqName);
    }

    @Nullable
    private NamespaceDescriptor computePackageFragment(@NotNull FqName fqName) {
        return new LazyJavaPackageFragment(module, fqName, new LazyJavaPackageFragmentScope());
    }


}

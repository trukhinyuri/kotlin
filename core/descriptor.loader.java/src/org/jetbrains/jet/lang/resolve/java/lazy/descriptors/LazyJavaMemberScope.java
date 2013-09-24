package org.jetbrains.jet.lang.resolve.java.lazy.descriptors;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.Computable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.java.JavaClassFinder;
import org.jetbrains.jet.lang.resolve.lazy.storage.NotNullLazyValue;
import org.jetbrains.jet.lang.resolve.lazy.storage.StorageManager;
import org.jetbrains.jet.lang.resolve.name.LabelName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.util.Collection;
import java.util.Collections;

public abstract class LazyJavaMemberScope implements JetScope {
    protected final JavaClassFinder finder;
    protected final StorageManager storageManager;

    private final DeclarationDescriptor containingDeclaration;
    private final NotNullLazyValue<Collection<DeclarationDescriptor>> allDescriptors;

    protected LazyJavaMemberScope(@NotNull DeclarationDescriptor containingDeclaration, @NotNull StorageManager storageManager, @NotNull JavaClassFinder finder) {
        this.finder = finder;
        this.containingDeclaration = containingDeclaration;
        this.storageManager = storageManager;

        this.allDescriptors = storageManager.createLazyValue(new Computable<Collection<DeclarationDescriptor>>() {
            @Override
            public Collection<DeclarationDescriptor> compute() {
                return computeAllDescriptors();
            }
        });

    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return containingDeclaration;
    }

    @Nullable
    @Override
    public ClassDescriptor getObjectDescriptor(@NotNull Name name) {
        // No object declarations in Java
        return null;
    }

    @NotNull
    @Override
    public Collection<ClassDescriptor> getObjectDescriptors() {
        // No object declarations in Java
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public NamespaceDescriptor getNamespace(@NotNull Name name) {
        // Should be resolved elsewhere
        return null;
    }

    @Nullable
    @Override
    public VariableDescriptor getLocalVariable(@NotNull Name name) {
        // Not a local scope
        return null;
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(@NotNull LabelName labelName) {
        // A member scope has no labels
        return Collections.emptySet();
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getOwnDeclaredDescriptors() {
        return getAllDescriptors();
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        return allDescriptors.compute();
    }

    private Collection<DeclarationDescriptor> computeAllDescriptors() {
        Collection<DeclarationDescriptor> result = Lists.newArrayList();

        for (Name name : getAllClassNames()) {
            DeclarationDescriptor descriptor = getClassifier(name);
            assert descriptor != null : "Descriptor not found for name " + name + " in " + containingDeclaration;
            result.add(descriptor);
        }

        for (Name name : getAllFunctionNames()) {
            result.addAll(getFunctions(name));
        }

        for (Name name : getAllPropertyNames()) {
            result.addAll(getProperties(name));
        }

        addExtraDescriptors(result);
        return result;
    }

    @NotNull
    protected abstract Collection<Name> getAllClassNames();

    @NotNull
    protected abstract Collection<Name> getAllPropertyNames();

    @NotNull
    protected abstract Collection<Name> getAllFunctionNames();

    protected abstract void addExtraDescriptors(@NotNull Collection<? super DeclarationDescriptor> result);

}

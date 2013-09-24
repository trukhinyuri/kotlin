package org.jetbrains.jet.lang.resolve.java.lazy.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.AbstractNamespaceDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorParent;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.util.Collections;

public class LazyJavaPackageFragment extends AbstractNamespaceDescriptorImpl implements NamespaceDescriptor, LazyJavaDescriptor {
    private final FqName fqName;
    private final JetScope memberScope;

    public LazyJavaPackageFragment(
            @NotNull NamespaceDescriptorParent containingDeclaration,
            @NotNull FqName fqName,
            @NotNull JetScope memberScope
    ) {
        super(containingDeclaration, Collections.<AnnotationDescriptor>emptyList(), fqName.shortName());
        this.fqName = fqName;
        this.memberScope = memberScope;
    }

    @NotNull
    @Override
    public JetScope getMemberScope() {
        return memberScope;
    }

    @NotNull
    @Override
    public FqName getFqName() {
        return fqName;
    }
}

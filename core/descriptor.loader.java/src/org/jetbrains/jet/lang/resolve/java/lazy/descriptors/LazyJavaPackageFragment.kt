package org.jetbrains.jet.lang.resolve.java.lazy.descriptors

import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor
import org.jetbrains.jet.lang.descriptors.impl.AbstractNamespaceDescriptorImpl
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorParent
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.utils.emptyList

public class LazyJavaPackageFragment(
        containingDeclaration: NamespaceDescriptorParent,
        private val _fqName: FqName,
        private val _memberScope: JetScope
) : AbstractNamespaceDescriptorImpl(containingDeclaration, emptyList(), _fqName.shortName()), NamespaceDescriptor, LazyJavaDescriptor {

    override fun getMemberScope() = _memberScope
    override fun getFqName() = _fqName
}

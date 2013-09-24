package org.jetbrains.jet.lang.resolve.java.lazy.descriptors

import org.jetbrains.jet.lang.resolve.lazy.storage.StorageManager
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.impl.ClassDescriptorBase

class LazyJavaClassDescriptor(
        storageManager: StorageManager,
        containingDeclaration: DeclarationDescriptor,
        fqName: FqName,
        javaClass: JavaClass
) : ClassDescriptorBase(containingDeclaration, fqName.shortName()), LazyJavaDescriptor {

}
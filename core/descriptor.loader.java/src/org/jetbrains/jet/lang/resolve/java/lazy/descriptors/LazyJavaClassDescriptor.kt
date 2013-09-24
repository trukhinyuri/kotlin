package org.jetbrains.jet.lang.resolve.java.lazy.descriptors

import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.resolve.lazy.storage.StorageManager
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor

class LazyJavaClassDescriptor(storageManager: StorageManager,
                              containingDeclaration: DeclarationDescriptor,
                              fqName: FqName,
                              javaClass: JavaClass) : ClassDescriptor, LazyJavaDescriptor {

}
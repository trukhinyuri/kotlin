package org.jetbrains.jet.lang.resolve.java.lazy.descriptors

import org.jetbrains.jet.lang.resolve.lazy.storage.StorageManager
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.impl.ClassDescriptorBase
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor
import org.jetbrains.jet.lang.types.TypeConstructor
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.jet.lang.resolve.java.resolver.JavaClassResolver
import org.jetbrains.jet.utils.emptyList
import org.jetbrains.jet.utils.emptyOrSingletonList

class LazyJavaClassDescriptor(
        storageManager: StorageManager,
        containingDeclaration: DeclarationDescriptor,
        fqName: FqName,
        private val javaClass: JavaClass
) : ClassDescriptorBase(containingDeclaration, fqName.shortName()), LazyJavaDescriptor {

    private val _kind = JavaClassResolver.determineClassKind(javaClass)
    private val _modality = JavaClassResolver.determineClassModality(javaClass)
    private val _visibility = javaClass.getVisibility()
    private val _isInner = JavaClassResolver.isInnerClass(javaClass)

    override fun getKind() = _kind
    override fun getModality() = _modality
    override fun getVisibility() = _visibility
    override fun isInner() = _isInner

    override fun getTypeConstructor(): TypeConstructor {
        throw UnsupportedOperationException()
    }

    override fun getScopeForMemberLookup(): JetScope? {
        throw UnsupportedOperationException()
    }

    override fun getThisAsReceiverParameter(): ReceiverParameterDescriptor {
        throw UnsupportedOperationException()
    }

    // TODO
    override fun getUnsubstitutedInnerClassesScope(): JetScope = JetScope.EMPTY

    override fun getUnsubstitutedPrimaryConstructor(): ConstructorDescriptor? = null

    // TODO
    override fun getConstructors() = emptyOrSingletonList(getUnsubstitutedPrimaryConstructor())

    // TODO
    override fun getClassObjectType(): JetType? = null

    // TODO
    override fun getClassObjectDescriptor(): ClassDescriptor? = null

    // TODO
    override fun getAnnotations(): MutableList<AnnotationDescriptor> = emptyList()

}
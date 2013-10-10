/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.resolve.java.scope;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.resolve.java.resolver.JavaMemberResolver;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;
import java.util.Collections;

import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.INCLUDE_KOTLIN_SOURCES;

public final class JavaClassStaticMembersScope extends JavaClassMembersScope {
    @NotNull
    private final FqName packageFQN;
    @NotNull
    private final JavaClass javaClass;

    public JavaClassStaticMembersScope(
            @NotNull NamespaceDescriptor descriptor,
            @NotNull FqName packageFQN,
            @NotNull JavaClass javaClass,
            @NotNull JavaMemberResolver memberResolver
    ) {
        super(descriptor, MembersProvider.forClass(javaClass, true), memberResolver);
        this.packageFQN = packageFQN;
        this.javaClass = javaClass;
    }

    @NotNull
    @Override
    protected Collection<DeclarationDescriptor> computeAllDescriptors() {
        Collection<DeclarationDescriptor> result = super.computeAllDescriptors();
        for (JavaClass nested : javaClass.getInnerClasses()) {
            // TODO 1 review
            PackageViewDescriptor namespace = getPackage(nested.getName());
            if (namespace != null) {
                result.add(namespace);
            }
        }
        return result;
    }

    @NotNull
    @Override
    protected Collection<ClassDescriptor> computeInnerClasses() {
        return Collections.emptyList();
    }
}

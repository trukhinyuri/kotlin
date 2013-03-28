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

package org.jetbrains.jet.lang.resolve.java.provider;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.PsiClassFinder;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;

import static org.jetbrains.jet.lang.resolve.java.provider.DeclarationOrigin.KOTLIN;

public final class KotlinPackagePsiDeclarationProvider extends ClassPsiDeclarationProviderImpl implements PackagePsiDeclarationProvider {
    @NotNull
    private final PsiPackage psiPackage;

    public KotlinPackagePsiDeclarationProvider(
            @NotNull PsiPackage psiPackage,
            @NotNull PsiClass psiClass,
            @NotNull PsiClassFinder psiClassFinder
    ) {
        super(psiClass, true, psiClassFinder);
        this.psiPackage = psiPackage;
    }

    @NotNull
    @Override
    public Collection<PsiClass> getAllPsiClasses() {
        return psiClassFinder.findPsiClasses(psiPackage);
    }

    @Nullable
    @Override
    public PsiClass getPsiClass(@NotNull Name name) {
        return psiClassFinder.findPsiClass(new FqName(psiPackage.getQualifiedName()).child(name)
        );
    }

    @NotNull
    @Override
    public PsiPackage getPsiPackage() {
        return psiPackage;
    }

    @NotNull
    @Override
    protected MembersCache buildMembersCache() {
        MembersCache cacheWithMembers = super.buildMembersCache();
        MembersCache.buildMembersByNameCache(cacheWithMembers, psiClassFinder, null, psiPackage, true, getDeclarationOrigin() == KOTLIN);
        return cacheWithMembers;
    }

    @NotNull
    @Override
    public DeclarationOrigin getDeclarationOrigin() {
        return KOTLIN;
    }
}
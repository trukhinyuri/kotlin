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

package org.jetbrains.jet.plugin.findUsages.handlers;

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.find.findUsages.JavaFindUsagesOptions;
import com.intellij.openapi.application.ReadActionProcessor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.plugin.findUsages.KotlinFindUsagesHandlerFactory;

import java.util.Collection;
import java.util.Collections;

public abstract class KotlinFindUsagesHandler<T extends PsiElement> extends FindUsagesHandler {
    private final KotlinFindUsagesHandlerFactory factory;
    private final Collection<? extends PsiElement> elementsToSearch;

    public KotlinFindUsagesHandler(
            @NotNull T psiElement,
            @NotNull Collection<? extends PsiElement> elementsToSearch,
            @NotNull KotlinFindUsagesHandlerFactory factory
    ) {
        super(psiElement);
        this.factory = factory;
        this.elementsToSearch = elementsToSearch;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public T getElement() {
        return (T)getPsiElement();
    }

    public KotlinFindUsagesHandler(@NotNull T psiElement, @NotNull KotlinFindUsagesHandlerFactory factory) {
        this(psiElement, Collections.<PsiElement>emptyList(), factory);
    }

    public final KotlinFindUsagesHandlerFactory getFactory() {
        return factory;
    }

    protected static boolean filterUsage(@NotNull PsiElement usage, @NotNull FindUsagesOptions options) {
        if (options instanceof JavaFindUsagesOptions && ((JavaFindUsagesOptions) options).isSkipImportStatements) {
            if (PsiTreeUtil.getParentOfType(usage, JetImportDirective.class) != null) return false;
        }

        return true;
    }

    protected static boolean processUsage(Processor<UsageInfo> processor, PsiReference ref, FindUsagesOptions options) {
        if (filterUsage(ref.getElement(), options)){
            TextRange rangeInElement = ref.getRangeInElement();
            return processor.process(new UsageInfo(ref.getElement(), rangeInElement.getStartOffset(), rangeInElement.getEndOffset(), false));
        }
        return true;
    }

    protected static boolean processUsage(
            @NotNull Processor<UsageInfo> processor,
            @NotNull PsiElement element,
            @NotNull FindUsagesOptions options
    ) {
        return !filterUsage(element, options) || processor.process(new UsageInfo(element));
    }

    @NotNull
    @Override
    public PsiElement[] getPrimaryElements() {
        return elementsToSearch.isEmpty()
               ? new PsiElement[] {getPsiElement()}
               : elementsToSearch.toArray(new PsiElement[elementsToSearch.size()]);
    }

    protected boolean searchReferences(
            @NotNull PsiElement element,
            @NotNull final Processor<UsageInfo> processor,
            @NotNull FindUsagesOptions options
    ) {
        if (options.isUsages) {
            boolean success = ReferencesSearch.search(
                    new ReferencesSearch.SearchParameters(element, options.searchScope, false, options.fastTrack)
            ).forEach(new ReadActionProcessor<PsiReference>() {
                @Override
                public boolean processInReadAction(PsiReference ref) {
                    TextRange rangeInElement = ref.getRangeInElement();
                    return processor.process(
                            new UsageInfo(ref.getElement(), rangeInElement.getStartOffset(), rangeInElement.getEndOffset(), false)
                    );
                }
            });
            if (!success) return false;
        }
        return true;
    }

    protected boolean searchTextOccurrences(
            @NotNull final PsiElement element,
            @NotNull final Processor<UsageInfo> processor,
            @NotNull FindUsagesOptions options
    ) {
        final SearchScope scope = options.searchScope;

        boolean searchText = options.isSearchForTextOccurrences && scope instanceof GlobalSearchScope;

        if (searchText) {
            if (options.fastTrack != null) {
                options.fastTrack.searchCustom(new Processor<Processor<PsiReference>>() {
                    @Override
                    public boolean process(Processor<PsiReference> consumer) {
                        return processUsagesInText(element, processor, (GlobalSearchScope)scope);
                    }
                });
            }
            else {
                return processUsagesInText(element, processor, (GlobalSearchScope)scope);
            }
        }
        return true;
    }

    @Override
    public boolean processElementUsages(
            @NotNull PsiElement element,
            @NotNull Processor<UsageInfo> processor,
            @NotNull FindUsagesOptions options
    ) {
        return searchReferences(element, processor, options) && searchTextOccurrences(element, processor, options);
    }
}

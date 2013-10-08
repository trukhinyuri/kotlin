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

package org.jetbrains.jet.plugin.refactoring.changeSignature;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.changeSignature.ChangeSignatureHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.refactoring.JetRefactoringBundle;

import static org.jetbrains.jet.plugin.refactoring.changeSignature.JetChangeSignatureUtil.findTargetForRefactoring;
import static org.jetbrains.jet.plugin.refactoring.changeSignature.JetChangeSignatureUtil.invokeChangeSignature;

public class JetChangeSignatureHandler implements ChangeSignatureHandler {
    @Nullable
    @Override
    public PsiElement findTargetMember(PsiFile file, Editor editor) {
        return findTargetMember(file.findElementAt(editor.getCaretModel().getOffset()));
    }

    @Nullable
    @Override
    public PsiElement findTargetMember(PsiElement element) {
        return findTargetForRefactoring(element);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file, DataContext dataContext) {
        editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
        PsiElement element = findTargetMember(file, editor);
        if (element == null) {
            element = LangDataKeys.PSI_ELEMENT.getData(dataContext);
        }

        if (element != null) {
            invokeChangeSignature(element, file.findElementAt(editor.getCaretModel().getOffset()), project, editor);
        }
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, @Nullable DataContext dataContext) {
        if (elements.length != 1) return;
        Editor editor = dataContext != null ? PlatformDataKeys.EDITOR.getData(dataContext) : null;
        invokeChangeSignature(elements[0], elements[0], project, editor);
    }

    @Nullable
    @Override
    public String getTargetNotFoundMessage() {
        return JetRefactoringBundle.message("error.wrong.caret.position.function.or.constructor.name");
    }
}

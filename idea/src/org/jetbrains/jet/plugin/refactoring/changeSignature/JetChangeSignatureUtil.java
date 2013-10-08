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

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.plugin.codeInsight.JetFunctionPsiElementCellRenderer;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class JetChangeSignatureUtil {

    private static void performRefactoringSilently(
            @NotNull final JetChangeSignatureDialog dialog,
            @NotNull final Project project,
            @Nullable final String commandName
    ) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                JetChangeInfo changeInfo = dialog.evaluateChangeInfo();
                JetChangeSignatureProcessor processor = new JetChangeSignatureProcessor(project, changeInfo, commandName);
                processor.run();
                Disposer.dispose(dialog.getDisposable());
            }
        });
    }

    public static void runChangeSignature(
            @NotNull Project project,
            @NotNull final JetFunctionPlatformDescriptor platformDescriptor,
            @NotNull PsiElement context,
            @Nullable String commandName,
            boolean performSilently
    ) {
        Collection<PsiElement> functionHierarchy = platformDescriptor.getFunctionHierarchy();
        FunctionDescriptor functionDescriptor = platformDescriptor.getDescriptor();
        assert functionDescriptor != null;
        Set<CallableMemberDescriptor> descriptors = BindingContextUtils.getDirectlyOverriddenDeclarations(functionDescriptor);
        List<PsiElement> overriddenVariants = ContainerUtil.map(descriptors, new Function<CallableMemberDescriptor, PsiElement>() {
            @Override
            public PsiElement fun(CallableMemberDescriptor descriptor) {
                BindingContext bindingContext = platformDescriptor.getBindingContext();
                return BindingContextUtils.callableDescriptorToDeclaration(bindingContext, descriptor);
            }
        });

        PsiElement dummy =
                PsiFileFactory.getInstance(project).createFileFromText("dummy", StdFileTypes.PLAIN_TEXT, "Whole hierarchy").getFirstChild();
        overriddenVariants.add(0, dummy);
        PsiElement[] elements = overriddenVariants.toArray(new PsiElement[overriddenVariants.size()]);
        NavigationUtil.getPsiElementPopup(elements, new DefaultPsiElementCellRenderer(), "Change signature of ...", new PsiElementProcessor<PsiElement>() {
            @Override
            public boolean execute(@NotNull PsiElement element) {
                //run refactoring
                return true;
            }
        }, dummy).showInFocusCenter();
        //if (functionHierarchy.size() != 1) {
        //    Messages.showYesNoCancelDialog()
        //}
        //JetChangeSignatureDialog dialog = new JetChangeSignatureDialog(project, platformDescriptor, context, commandName);
        //if (performSilently || ApplicationManager.getApplication().isUnitTestMode()) {
        //    performRefactoringSilently(dialog, project, commandName);
        //}
        //else {
        //    dialog.show();
        //}
    }

    private enum Options {
        PROCESS_SUPER_METHODS
    }

    private JetChangeSignatureUtil() {}
}

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

import com.google.common.collect.Sets;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.searches.OverridingMethodsSearch;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.jetAsJava.JetClsMethod;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.BindingContextUtils.callableDescriptorToDeclaration;
import static org.jetbrains.jet.lang.resolve.BindingContextUtils.getAllOverriddenDeclarations;

public class JetFunctionPlatformDescriptorImpl implements JetFunctionPlatformDescriptor {
    private final FunctionDescriptor funDescriptor;
    private final PsiElement funElement;
    private final List<JetParameterInfo> parameters;
    @NotNull
    private final BindingContext bindingContext;

    public JetFunctionPlatformDescriptorImpl(FunctionDescriptor descriptor, PsiElement element, @NotNull BindingContext bindingContext) {
        funDescriptor = descriptor;
        funElement = element;
        this.bindingContext = bindingContext;
        final List<JetParameter> valueParameters = funElement instanceof JetFunction
                                                   ? ((JetFunction) funElement).getValueParameters()
                                                   : ((JetClass) funElement).getPrimaryConstructorParameters();
        parameters = new ArrayList<JetParameterInfo>(
                ContainerUtil.map(funDescriptor.getValueParameters(), new Function<ValueParameterDescriptor, JetParameterInfo>() {
                    @Override
                    public JetParameterInfo fun(ValueParameterDescriptor param) {
                        JetParameter parameter = valueParameters.get(param.getIndex());
                        return new JetParameterInfo(param.getIndex(), param.getName().asString(), param.getType(),
                                                    parameter.getDefaultValue(), parameter.getValOrVarNode());
                    }
                }));
    }

    @Override
    public String getName() {
        if (funDescriptor instanceof ConstructorDescriptor) {
            return funDescriptor.getContainingDeclaration().getName().asString();
        }
        else if (funDescriptor instanceof AnonymousFunctionDescriptor) {
            return "";
        }
        else {
            return funDescriptor.getName().asString();
        }
    }

    @Override
    public List<JetParameterInfo> getParameters() {
        return parameters;
    }

    public void addParameter(JetParameterInfo parameter) {
        parameters.add(parameter);
    }

    public void removeParameter(int index) {
        parameters.remove(index);
    }

    public void clearParameters() {
        parameters.clear();
    }

    @Override
    public int getParametersCount() {
        return funDescriptor.getValueParameters().size();
    }

    @Override
    public Visibility getVisibility() {
        return funDescriptor.getVisibility();
    }

    @Override
    public PsiElement getMethod() {
        return funElement;
    }

    @NotNull
    @Override
    public PsiElement getContext() {
        return funElement;
    }

    @Override
    public boolean isConstructor() {
        return funDescriptor instanceof ConstructorDescriptor;
    }

    @Override
    public boolean canChangeVisibility() {
        DeclarationDescriptor parent = funDescriptor.getContainingDeclaration();
        return !(funDescriptor instanceof AnonymousFunctionDescriptor ||
                 parent instanceof ClassDescriptor && ((ClassDescriptor) parent).getKind() == ClassKind.TRAIT);
    }

    @Override
    public boolean canChangeParameters() {
        return true;
    }

    @Override
    public boolean canChangeName() {
        return !(funDescriptor instanceof ConstructorDescriptor || funDescriptor instanceof AnonymousFunctionDescriptor);
    }

    @Override
    public ReadWriteOption canChangeReturnType() {
        return isConstructor() ? ReadWriteOption.None : ReadWriteOption.ReadWrite;
    }

    @Override
    public FunctionDescriptor getDescriptor() {
        return funDescriptor;
    }

    @NotNull
    @Override
    public Collection<PsiElement> getFunctionHierarchy() {
        Set<PsiElement> result = Sets.newHashSet();
        result.addAll(computeOverriddenDeclarations());
        result.addAll(computeOverridingDeclarations());
        result.add(funElement);
        return result;
    }

    @NotNull
    private Collection<PsiElement> computeOverriddenDeclarations() {
        Collection<CallableMemberDescriptor> overriddenDeclarations = getAllOverriddenDeclarations(funDescriptor);
        Set<PsiElement> result = Sets.newHashSet();
        for (CallableMemberDescriptor overriddenDeclaration : overriddenDeclarations) {
            result.add(callableDescriptorToDeclaration(bindingContext, overriddenDeclaration));
        }
        return result;
    }

    @NotNull
    private Collection<JetDeclaration> computeOverridingDeclarations() {
        if (!(funElement instanceof JetNamedFunction)) {
            return Collections.emptySet();
        }
        PsiMethod lightMethod = LightClassUtil.getLightClassMethod((JetNamedFunction) funElement);
        // there are valid situations when light method is null: local functions and literals
        if (lightMethod == null) {
            return Collections.emptySet();
        }
        Collection<PsiMethod> overridingMethods = OverridingMethodsSearch.search(lightMethod).findAll();
        List<PsiMethod> jetLightMethods = ContainerUtil.filter(overridingMethods, new Condition<PsiMethod>() {
            @Override
            public boolean value(PsiMethod method) {
                return method instanceof JetClsMethod;
            }
        });
        return ContainerUtil.map(jetLightMethods, new Function<PsiMethod, JetDeclaration>() {
            @Override
            public JetDeclaration fun(PsiMethod method) {
                return ((JetClsMethod) method).getOrigin();
            }
        });
    }

    @Override
    @Nullable
    public String getReturnTypeText() {
        JetType returnType = funDescriptor.getReturnType();
        return returnType != null ? DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(returnType) : null;
    }
}

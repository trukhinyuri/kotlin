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

package org.jetbrains.jet.plugin.compiler.configuration;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.arguments.K2JSCompilerArguments;

import static org.jetbrains.jet.plugin.compiler.SettingConstants.KOTLIN_COMPILER_SETTINGS_PATH;
import static org.jetbrains.jet.plugin.compiler.SettingConstants.KOTLIN_TO_JS_COMPILER_SETTINGS_SECTION;

@State(
    name = KOTLIN_TO_JS_COMPILER_SETTINGS_SECTION,
    storages = {
        @Storage(file = StoragePathMacros.PROJECT_FILE),
        @Storage(file = KOTLIN_COMPILER_SETTINGS_PATH, scheme = StorageScheme.DIRECTORY_BASED)
    }
)
public class Kotlin2JsCompilerSettings extends BaseKotlinCompilerSettings<K2JSCompilerArguments> implements PersistentStateComponent<Element> {

    @NotNull
    @Override
    protected K2JSCompilerArguments createSettings() {
        return new K2JSCompilerArguments();
    }

    public static Kotlin2JsCompilerSettings getInstance(Project project) {
        return ServiceManager.getService(project, Kotlin2JsCompilerSettings.class);
    }
}

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

package org.jetbrains.jet.cli.common.arguments;

import com.sampullara.cli.Argument;

import static org.jetbrains.jet.cli.common.arguments.ArgumentConstants.SUPPRESS_WARNINGS;

public abstract class CommonCompilerArguments extends CompilerArguments {
    public static final CommonCompilerArguments DUMMY = new CommonCompilerArguments() {
        @Override
        public String getSrc() {
            return null;
        }
    };

    @Argument(value = "tags", description = "Demarcate each compilation message (error, warning, etc) with an open and close tag")
    public boolean tags;
    @Argument(value = "verbose", description = "Enable verbose logging output")
    public boolean verbose;
    @Argument(value = "version", description = "Display compiler version")
    public boolean version;
    @Argument(value = "help", alias = "h", description = "Show help")
    public boolean help;
    @Argument(value = "suppress", description = "Suppress compiler messages by severity (warnings)")
    public String suppress;

    @Override
    public boolean isHelp() {
        return help;
    }

    public void setHelp(boolean help) {
        this.help = help;
    }

    @Override
    public boolean isTags() {
        return tags;
    }

    @Override
    public boolean isVersion() {
        return version;
    }

    @Override
    public boolean isVerbose() {
        return verbose;
    }

    public void setTags(boolean tags) {
        this.tags = tags;
    }

    @Override
    public boolean suppressAllWarnings() {
        return SUPPRESS_WARNINGS.equalsIgnoreCase(suppress);
    }
}

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend

import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.DirectivesContainer
import org.jetbrains.kotlin.test.model.BackendKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.model.moduleStructure
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.util.StringUtils.joinToArrayString

class BlackBoxCodegenSuppressor(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override val directives: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    override fun suppressErrors(failedAssertions: List<AssertionError>): List<AssertionError> {
        val moduleStructure = testServices.moduleStructure
        val ignoredBackends = moduleStructure.modules.flatMap { it.directives[CodegenTestDirectives.ignoreBackend] }
        if (ignoredBackends.isEmpty()) return failedAssertions
        val targetBackends = moduleStructure.modules.flatMap { it.targetBackends }
        val matchedBackend = ignoredBackends.intersect(targetBackends)
        if (ignoredBackends.contains(TargetBackend.ANY)) {
            return processAssertions(failedAssertions)
        }
        if (matchedBackend.isNotEmpty()) {
            return processAssertions(failedAssertions, "for ${matchedBackend.joinToArrayString()}")
        }
        return failedAssertions
    }

    private fun processAssertions(failedAssertions: List<AssertionError>, additionalMessage: String = ""): List<AssertionError> {
        return if (failedAssertions.isNotEmpty()) emptyList()
        else {
            val message = buildString {
                append("Looks like this test can be unmuted. Remove IGNORE_BACKEND directive")
                if (additionalMessage.isNotEmpty()) {
                    append(" ")
                    append(additionalMessage)
                }
            }
            listOf(AssertionError(message))
        }
    }

    private val TestModule.targetBackends: List<TargetBackend>
        get() = when (backendKind) {
            BackendKind.ClassicBackend -> when {
                targetPlatform.isJvm() -> listOf(TargetBackend.JVM, TargetBackend.JVM_OLD)
                targetPlatform.isJs() -> listOf(TargetBackend.JS)
                else -> emptyList()
            }
            BackendKind.IrBackend -> when {
                targetPlatform.isJvm() -> listOf(TargetBackend.JVM_IR)
                targetPlatform.isJs() -> listOf(TargetBackend.JS_IR)
                else -> emptyList()
            }
            else -> emptyList()
        }
}

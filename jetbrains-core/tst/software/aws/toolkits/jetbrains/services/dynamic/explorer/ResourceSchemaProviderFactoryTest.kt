// Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.dynamic.explorer

import com.intellij.testFramework.DisposableRule
import com.intellij.testFramework.runInEdtAndWait
import com.jetbrains.jsonSchema.impl.inspections.JsonSchemaComplianceInspection
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import software.aws.toolkits.core.credentials.aToolkitCredentialsProvider
import software.aws.toolkits.core.region.anAwsRegion
import software.aws.toolkits.jetbrains.core.MockResourceCacheRule
import software.aws.toolkits.jetbrains.core.credentials.ConnectionSettings
import software.aws.toolkits.jetbrains.core.fillResourceCache
import software.aws.toolkits.jetbrains.services.dynamic.DynamicResource
import software.aws.toolkits.jetbrains.services.dynamic.DynamicResourceIdentifier
import software.aws.toolkits.jetbrains.services.dynamic.DynamicResourceSchemaMapping
import software.aws.toolkits.jetbrains.services.dynamic.ResourceType
import software.aws.toolkits.jetbrains.services.dynamic.ViewEditableDynamicResourceVirtualFile
import software.aws.toolkits.jetbrains.utils.rules.JavaCodeInsightTestFixtureRule

class ResourceSchemaProviderFactoryTest {
    @Rule
    @JvmField
    val projectRule = JavaCodeInsightTestFixtureRule()

    @Rule
    @JvmField
    val disposableRule = DisposableRule()

    @JvmField
    @Rule
    val resourceCache = MockResourceCacheRule()

    @Before
    fun setup() {
        resourceCache.fillResourceCache(projectRule.project)
    }

    private val resource = DynamicResource(ResourceType("AWS::Log::LogGroup", "Log", "LogGroup"), "sampleIdentifier")

    @Test
    fun `Check whether schema is applied`() {
        val fixture = projectRule.fixture
        val jsonSchemaComplianceInspection = JsonSchemaComplianceInspection()

        try {
            fixture.enableInspections(jsonSchemaComplianceInspection)
            val file = ViewEditableDynamicResourceVirtualFile(
                DynamicResourceIdentifier(ConnectionSettings(aToolkitCredentialsProvider(), anAwsRegion()), resource.type.fullName, resource.identifier),
                """
                {"RetentionInDays":<warning descr="Value should be one of: 1, 3, 5, 7, 14, 30, 60, 90, 120, 150, 180, 365, 400, 545, 731, 1827, 3653">18</warning>}
                """.trimIndent()
            )
            DynamicResourceSchemaMapping.getInstance().addResourceSchemaMapping(projectRule.project, file)
            runInEdtAndWait {
                fixture.openFileInEditor(file)
                fixture.checkHighlighting()
            }
        } finally {
            fixture.disableInspections(jsonSchemaComplianceInspection)
        }
    }
}

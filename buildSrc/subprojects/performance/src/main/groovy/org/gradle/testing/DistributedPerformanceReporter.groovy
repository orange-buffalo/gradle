/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.testing

import com.google.common.annotations.VisibleForTesting
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import org.apache.commons.io.FileUtils
import org.gradle.api.internal.ProcessOperations
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.internal.tasks.testing.junit.result.TestClassResult
import org.gradle.api.internal.tasks.testing.junit.result.TestResultSerializer

import javax.inject.Inject
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicLong

@CompileStatic
class DistributedPerformanceReporter extends DefaultPerformanceReporter {
    /**
     * If the reporter supports rerun.
     *
     * For the "rerun-able" distributed performance test, the report will only be generated when the first run fails,
     * or the rerun finishes. For "non-rerun-able" performance test, the report will be generated when the first run finishes.
     */
    boolean rerunable

    @Inject
    DistributedPerformanceReporter(ProcessOperations processOperations, FileOperations fileOperations) {
        super(processOperations, fileOperations)
    }

    @Override
    void report(PerformanceTest performanceTest) {
        DistributedPerformanceTest distributedPerformanceTest = (DistributedPerformanceTest) performanceTest

        if (isGeneratingScenarioList(distributedPerformanceTest)) {
            // do nothing
        } else if (!distributedPerformanceTest.isRerun()) {
            // first run, only write report when it succeeds
            if (allWorkerBuildsAreSuccessful(distributedPerformanceTest) || !rerunable) {
                super.report(distributedPerformanceTest)
            } else {
                writeBinaryResults(distributedPerformanceTest)
                generateResultsJson(distributedPerformanceTest)
            }
        } else {
            super.report(distributedPerformanceTest)
        }
    }

    @Override
    protected void generateResultsJson(PerformanceTest performanceTest) {
        DistributedPerformanceTest distributedPerformanceTest = (DistributedPerformanceTest) performanceTest

        List<ScenarioBuildResultData> resultData = distributedPerformanceTest.isRerun() ? getResultsFromCurrentRun(distributedPerformanceTest) + getResultsFromPreviousRun() : getResultsFromCurrentRun(distributedPerformanceTest)
        FileUtils.write(resultsJson, JsonOutput.toJson(resultData), Charset.defaultCharset())
    }

    static boolean isGeneratingScenarioList(DistributedPerformanceTest distributedPerformanceTest) {
        return distributedPerformanceTest.finishedBuilds.isEmpty()
    }

    static boolean allWorkerBuildsAreSuccessful(DistributedPerformanceTest distributedPerformanceTest) {
        return distributedPerformanceTest.finishedBuilds.values().every { it.successful }
    }

    /**
     * This is for tagging plugin. See https://github.com/gradle/ci-health/blob/3e30ea146f594ee54a4efe4384f933534b40739c/gradle-build-tag-plugin/src/main/groovy/org/gradle/ci/tagging/plugin/TagSingleBuildPlugin.groovy
     */
    @VisibleForTesting
    static void writeBinaryResults(DistributedPerformanceTest distributedPerformanceTest) {
        AtomicLong counter = new AtomicLong()
        Map<String, List<DistributedPerformanceTest.ScenarioResult>> classNameToScenarioNames = distributedPerformanceTest.finishedBuilds.values().findAll { it.testClassFullName != null }.groupBy {
            it.testClassFullName
        }
        List<TestClassResult> classResults = classNameToScenarioNames.entrySet().collect { Map.Entry<String, List<DistributedPerformanceTest.ScenarioResult>> entry ->
            TestClassResult classResult = new TestClassResult(counter.incrementAndGet(), entry.key, 0L)
            entry.value.each { DistributedPerformanceTest.ScenarioResult scenarioResult ->
                classResult.add(scenarioResult.toMethodResult(counter))
            }
            classResult
        }

        new TestResultSerializer(distributedPerformanceTest.binResultsDir).write(classResults)
    }

    private List<ScenarioBuildResultData> getResultsFromPreviousRun() {
        return resultsJson.isFile() ? ((List<Map>) new JsonSlurper().parseText(resultsJson.text)).collect { new ScenarioBuildResultData(it) } : []
    }

    @TypeChecked(TypeCheckingMode.SKIP)
    private static List<ScenarioBuildResultData> getResultsFromCurrentRun(DistributedPerformanceTest distributedPerformanceTest) {
        return distributedPerformanceTest.finishedBuilds.collect { workerBuildId, scenarioResult ->
            new ScenarioBuildResultData(
                teamCityBuildId: workerBuildId,
                scenarioName: distributedPerformanceTest.scheduledBuilds.get(workerBuildId).id,
                scenarioClass: scenarioResult.testClassFullName,
                webUrl: scenarioResult.buildResponse.webUrl,
                status: scenarioResult.buildResponse.status,
                agentName: scenarioResult.buildResponse.agent.name,
                agentUrl: scenarioResult.buildResponse.agent.webUrl,
                testFailure: scenarioResult.failureText)
        }
    }
}

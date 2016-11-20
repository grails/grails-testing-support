/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.grails.testing

import grails.testing.runtime.FreshRuntime
import groovy.transform.CompileStatic
import org.grails.testing.runtime.TestRuntime
import org.grails.testing.runtime.TestRuntimeFactory
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

@CompileStatic
class TestRuntimeRule implements TestRule {

    def testInstance

    @Override
    Statement apply(Statement statement, Description description) {
        return new Statement() {
            public void evaluate() throws Throwable {
                Class testClass = description.getTestClass()
                TestRuntime runtime = TestRuntimeFactory.getRuntimeForTestClass(testClass)
                def eventArguments = [testInstance: testInstance]
                handleFreshContextAnnotation(runtime, description)
                runtime.publishEvent("before", eventArguments, [immediateDelivery: true])
                try {
                    statement.evaluate()
                } catch (Throwable t) {
                    throw t
                } finally {
                    runtime.publishEvent("after", [testInstance: testInstance], [immediateDelivery: true, reverseOrderDelivery: true])
                }
            }
        }
    }

    protected handleFreshContextAnnotation(TestRuntime runtime, Description description, Map eventArguments = [:]) {
        if (doesRequireFreshContext(description)) {
            runtime.publishEvent('requestFreshRuntime', eventArguments, [immediateDelivery: true])
        }
    }

    protected boolean doesRequireFreshContext(Description testDescription) {
        if (testDescription?.getAnnotation(FreshRuntime) || testDescription?.getTestClass()?.isAnnotationPresent(FreshRuntime)) {
            return true
        }
        return false
    }
}

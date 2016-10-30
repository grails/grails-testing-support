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

import grails.core.GrailsApplication
import grails.test.runtime.TestRuntime
import grails.test.runtime.TestRuntimeFactory
import org.springframework.context.ConfigurableApplicationContext

trait GrailsUnitTest<T> {

    private TestRuntime currentRuntime;

    ConfigurableApplicationContext getApplicationContext() {
        getMainContext()
    }

    ConfigurableApplicationContext getMainContext() {
        (ConfigurableApplicationContext) grailsApplication.mainContext
    }

    GrailsApplication getGrailsApplication() {
        (GrailsApplication) runtime.getValue("grailsApplication")
    }

    public TestRuntime getRuntime() {
        if (currentRuntime == null) {
            currentRuntime = TestRuntimeFactory.getRuntimeForTestClass(this.class);
        }
        if (currentRuntime == null) {
            throw new IllegalStateException("Current TestRuntime instance is null.");
        } else if (currentRuntime.isClosed()) {
            throw new IllegalStateException("Current TestRuntime instance is closed.");
        }
        return currentRuntime;
    }

    public void setRuntime(TestRuntime runtime) {
        this.currentRuntime = runtime;
    }

    void defineBeans(boolean immediateDelivery = true, Closure<?> closure) {
        runtime.publishEvent("defineBeans", [closure: closure], [immediateDelivery: immediateDelivery])
    }
}

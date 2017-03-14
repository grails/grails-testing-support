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
package org.grails.testing.spock

import grails.testing.spring.AutowiredTest
import groovy.util.logging.Slf4j
import org.spockframework.runtime.extension.AbstractGlobalExtension
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation
import org.spockframework.runtime.model.SpecInfo

@Slf4j
class AutowiredTestExtension extends AbstractGlobalExtension {

    void start() {
        log.debug "start..."
    }

    void visitSpec(SpecInfo spec) {
        log.debug "visitSpec..."
        spec.addSetupInterceptor(new AutowiredSetupInterceptor())
    }

    void stop() {
        log.debug "stop..."
    }
}

@Slf4j
class AutowiredSetupInterceptor implements IMethodInterceptor {

    @Override
    void intercept(IMethodInvocation invocation) throws Throwable {
        def theInstance = invocation.instance
        if(theInstance instanceof AutowiredTest) {
            theInstance.autowire()
        }
        log.debug "intercept..."
        invocation.proceed()
    }
}


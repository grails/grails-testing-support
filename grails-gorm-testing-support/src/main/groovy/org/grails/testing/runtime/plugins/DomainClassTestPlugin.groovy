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

package org.grails.testing.runtime.plugins

import grails.core.GrailsApplication
import grails.validation.ConstrainedProperty
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.grails.datastore.gorm.validation.constraints.UniqueConstraintFactory
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.grails.testing.runtime.TestEvent
import org.grails.testing.runtime.TestPlugin
import org.grails.testing.runtime.TestRuntime
import org.springframework.context.ConfigurableApplicationContext

/**
 * a TestPlugin for TestRuntime for adding Grails DomainClass (GORM) support
 * - this implementation uses SimpleMapDatastore GORM implementation
 * 
 *
 */
@CompileStatic
class DomainClassTestPlugin implements TestPlugin {
    String[] requiredFeatures = ['grailsApplication', 'coreBeans']
    String[] providedFeatures = ['domainClass']
    int ordinal = 0

    protected void applicationInitialized(TestRuntime runtime, GrailsApplication grailsApplication) {
    }
    
    protected void cleanupDatastore() {
    }

    void defineBeans(TestRuntime runtime, Closure closure) {
        runtime.publishEvent("defineBeans", [closure: closure])
    }
    
    GrailsApplication getGrailsApplication(TestEvent event) {
        (GrailsApplication)event.runtime.getValue("grailsApplication")
    }

    public void onTestEvent(TestEvent event) {
    }
    
    public void close(TestRuntime runtime) {
        
    }
}

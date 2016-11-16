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

import groovy.transform.CompileStatic
import org.springframework.beans.factory.config.AutowireCapableBeanFactory

import java.lang.reflect.ParameterizedType

@CompileStatic
trait ParameterizedGrailsUnitTest<T> extends GrailsUnitTest {

    private artefactInstance


    private Class<T> getTypeUnderTest() {
        ParameterizedType parameterizedType = (ParameterizedType)getClass().genericInterfaces.find { genericInterface ->
            genericInterface instanceof ParameterizedType &&
              ParameterizedGrailsUnitTest.isAssignableFrom((Class)((ParameterizedType)genericInterface).rawType)
        }

        parameterizedType?.actualTypeArguments[0]
    }

    def getArtefactInstance() {
        if (artefactInstance == null && applicationContext != null) {
            def cutType = getTypeUnderTest()
            mockArtefact(cutType)
            if (this.getApplicationContext().containsBean(cutType.name)) {
                artefactInstance = applicationContext.getBean(cutType.name)
            } else {
                artefactInstance = cutType.newInstance()
            }

            applicationContext.autowireCapableBeanFactory.autowireBeanProperties artefactInstance, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false
        }
        artefactInstance
    }
}

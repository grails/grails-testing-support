package org.grails.testing.gorm.spock

import groovy.transform.CompileStatic
import org.spockframework.runtime.extension.AbstractGlobalExtension
import org.spockframework.runtime.model.SpecInfo

@CompileStatic
class DataTestExtension extends AbstractGlobalExtension {

    void visitSpec(SpecInfo spec) {
//        spec.addSetupInterceptor(new DataTestSetupExtension())
        spec.addSetupSpecInterceptor(new DataTestSetupExtension())
        spec.allFeatures*.addInterceptor(new DataTestSetupExtension())
//        spec.addInitializerInterceptor(new DataTestSetupExtension())
//        spec.addI
    }
}

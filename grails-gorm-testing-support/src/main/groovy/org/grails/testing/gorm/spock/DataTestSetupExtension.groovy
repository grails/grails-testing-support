package org.grails.testing.gorm.spock

import grails.testing.gorm.DataTest
import grails.validation.ConstrainedProperty
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.validation.constraints.UniqueConstraintFactory
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.spockframework.runtime.IRunListener
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation
import org.spockframework.runtime.model.ErrorInfo
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IterationInfo
import org.spockframework.runtime.model.SpecInfo
import org.springframework.context.ConfigurableApplicationContext

@CompileStatic
class DataTestSetupExtension implements IMethodInterceptor, IRunListener{

    @Override
    void intercept(IMethodInvocation invocation) throws Throwable {
        def theInstance = invocation.instance
        if(theInstance instanceof DataTest) {
            configureDataTest theInstance
        }
        invocation.proceed()
    }

    @CompileDynamic
    void setupDataTestBeans(DataTest testInstance) {
        testInstance.defineBeans(true) {
            grailsDatastore SimpleMapDatastore, testInstance.grailsApplication.mainContext
            transactionManager(DatastoreTransactionManager) {
                datastore = ref('grailsDatastore')
            }
        }
    }

    void configureDataTest(DataTest testInstance) {
        setupDataTestBeans testInstance
        ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext)testInstance.grailsApplication.mainContext
        SimpleMapDatastore simpleDatastore = applicationContext.getBean(SimpleMapDatastore)
        ConstrainedProperty.registerNewConstraint('unique', new UniqueConstraintFactory(simpleDatastore))
        DatastoreUtils.bindSession(simpleDatastore.connect())
        if (!testInstance.domainsHaveBeenMocked) {
            def classes = testInstance.domainClassesToMock
            if (classes) {
                testInstance.mockDomains classes
            }
            testInstance.domainsHaveBeenMocked = true
        }
    }

    @Override
    void beforeSpec(SpecInfo spec) {
    }

    @Override
    void beforeFeature(FeatureInfo feature) {
    }

    @Override
    void beforeIteration(IterationInfo iteration) {

    }

    @Override
    void afterIteration(IterationInfo iteration) {

    }

    @Override
    void afterFeature(FeatureInfo feature) {

    }

    @Override
    void afterSpec(SpecInfo spec) {

    }

    @Override
    void error(ErrorInfo error) {

    }

    @Override
    void specSkipped(SpecInfo spec) {

    }

    @Override
    void featureSkipped(FeatureInfo feature) {

    }
}

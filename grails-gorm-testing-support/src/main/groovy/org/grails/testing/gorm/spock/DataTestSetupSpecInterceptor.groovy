package org.grails.testing.gorm.spock

import grails.testing.gorm.DataTest
import grails.validation.ConstrainedProperty
import grails.validation.ConstraintsEvaluator
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.validation.constraints.UniqueConstraintFactory
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.grails.plugins.domain.DomainClassGrailsPlugin
import org.grails.validation.ConstraintsEvaluatorFactoryBean
import org.spockframework.runtime.IRunListener
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation
import org.spockframework.runtime.model.ErrorInfo
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.IterationInfo
import org.spockframework.runtime.model.SpecInfo
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext

@CompileStatic
class DataTestSetupSpecInterceptor implements IMethodInterceptor {

    @Override
    void intercept(IMethodInvocation invocation) throws Throwable {
        configureDataTest((DataTest)invocation.instance)
        invocation.proceed()
    }

    @CompileDynamic
    void setupDataTestBeans(DataTest testInstance) {
        testInstance.defineBeans {
            "${ConstraintsEvaluator.BEAN_NAME}"(ConstraintsEvaluatorFactoryBean) {
                defaultConstraints = DomainClassGrailsPlugin.getDefaultConstraints(application.config)
            }
            grailsDatastore SimpleMapDatastore, application.mainContext
            transactionManager(DatastoreTransactionManager) {
                datastore = ref('grailsDatastore')
            }
        }
    }

    void configureDataTest(DataTest testInstance) {
        setupDataTestBeans testInstance
        ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext)testInstance.applicationContext
        SimpleMapDatastore simpleDatastore = applicationContext.getBean(SimpleMapDatastore)
        ConstrainedProperty.registerNewConstraint('unique', new UniqueConstraintFactory(simpleDatastore))
        if (!testInstance.domainsHaveBeenMocked) {
            def classes = testInstance.domainClassesToMock
            if (classes) {
                testInstance.mockDomains classes
            }
            testInstance.domainsHaveBeenMocked = true
        }
    }

}

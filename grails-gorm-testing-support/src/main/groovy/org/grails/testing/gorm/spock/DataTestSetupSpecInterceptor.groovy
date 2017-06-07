package org.grails.testing.gorm.spock

import grails.testing.gorm.DataTest
import grails.validation.ConstrainedProperty
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.validation.constraints.UniqueConstraintFactory
import org.grails.datastore.gorm.validation.constraints.registry.DefaultConstraintRegistry
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.reflect.ClassUtils
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.grails.validation.ConstraintEvalUtils
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation
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

        Class constraintsEvaluator

        boolean oldSetup = false
        if (ClassUtils.isPresent("org.grails.validation.ConstraintsEvaluatorFactoryBean")) {
            constraintsEvaluator = ClassUtils.forName("org.grails.validation.ConstraintsEvaluatorFactoryBean")
            if (constraintsEvaluator.getAnnotation(Deprecated) == null) {
                oldSetup = true
            }
        }

        testInstance.defineBeans {
            grailsDatastore SimpleMapDatastore, application.mainContext

            if (oldSetup) {
                "org.grails.beans.ConstraintsEvaluator"(constraintsEvaluator) {
                    defaultConstraints = ConstraintEvalUtils.getDefaultConstraints(application.config)
                }
            } else {
                constraintsEvaluator = ClassUtils.forName("org.grails.datastore.gorm.validation.constraints.eval.DefaultConstraintEvaluator")
                constraintRegistry(DefaultConstraintRegistry, ref("messageSource"))
                grailsDomainClassMappingContext(grailsDatastore: "getMappingContext")

                "org.grails.beans.ConstraintsEvaluator"(constraintsEvaluator, constraintRegistry, grailsDomainClassMappingContext, ConstraintEvalUtils.getDefaultConstraints(application.config))
            }

            transactionManager(DatastoreTransactionManager) {
                datastore = ref('grailsDatastore')
            }
        }

        if (!oldSetup) {
            testInstance.grailsApplication.setMappingContext(
                    testInstance.applicationContext.getBean('grailsDomainClassMappingContext', MappingContext)
            )
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

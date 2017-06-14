package org.grails.testing.gorm.spock

import grails.testing.gorm.DataTest
import grails.validation.ConstrainedProperty
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.validation.constraints.UniqueConstraintFactory
import org.grails.datastore.gorm.validation.constraints.builtin.UniqueConstraint
import org.grails.datastore.gorm.validation.constraints.registry.ConstraintRegistry
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

    public static Boolean IS_OLD_SETUP = false
    public static final BEAN_NAME = "org.grails.beans.ConstraintsEvaluator"
    private static Class constraintsEvaluator

    static {
        if (ClassUtils.isPresent("org.grails.validation.ConstraintsEvaluatorFactoryBean")) {
            constraintsEvaluator = ClassUtils.forName("org.grails.validation.ConstraintsEvaluatorFactoryBean")
            if (constraintsEvaluator.getAnnotation(Deprecated) == null) {
                IS_OLD_SETUP = true
            }
        }
        if (!IS_OLD_SETUP) {
            constraintsEvaluator = ClassUtils.forName("org.grails.datastore.gorm.validation.constraints.eval.DefaultConstraintEvaluator")
        }
    }

    @Override
    void intercept(IMethodInvocation invocation) throws Throwable {
        configureDataTest((DataTest)invocation.instance)
        invocation.proceed()
    }

    @CompileDynamic
    void setupDataTestBeans(DataTest testInstance) {

        testInstance.defineBeans {
            grailsDatastore SimpleMapDatastore, application.mainContext

            if (IS_OLD_SETUP) {
                "${BEAN_NAME}"(constraintsEvaluator) {
                    defaultConstraints = ConstraintEvalUtils.getDefaultConstraints(application.config)
                }
            } else {
                constraintRegistry(DefaultConstraintRegistry, ref("messageSource"))
                grailsDomainClassMappingContext(grailsDatastore: "getMappingContext")

                "${BEAN_NAME}"(constraintsEvaluator, constraintRegistry, grailsDomainClassMappingContext, ConstraintEvalUtils.getDefaultConstraints(application.config))
            }

            transactionManager(DatastoreTransactionManager) {
                datastore = ref('grailsDatastore')
            }
        }

        if (!IS_OLD_SETUP) {
            testInstance.grailsApplication.setMappingContext(
                    testInstance.applicationContext.getBean('grailsDomainClassMappingContext', MappingContext)
            )
        }
    }

    void configureDataTest(DataTest testInstance) {
        setupDataTestBeans testInstance
        ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext)testInstance.applicationContext
        SimpleMapDatastore simpleDatastore = applicationContext.getBean(SimpleMapDatastore)

        if (IS_OLD_SETUP) {
            ConstrainedProperty.registerNewConstraint('unique', new UniqueConstraintFactory(simpleDatastore))
        } else {
            applicationContext.getBean('constraintRegistry', ConstraintRegistry).addConstraint(UniqueConstraint)
        }

        if (!testInstance.domainsHaveBeenMocked) {
            def classes = testInstance.domainClassesToMock
            if (classes) {
                testInstance.mockDomains classes
            }
            testInstance.domainsHaveBeenMocked = true
        }
    }

}

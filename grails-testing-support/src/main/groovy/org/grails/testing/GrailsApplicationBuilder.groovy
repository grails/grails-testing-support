package org.grails.testing

import grails.boot.GrailsApp
import grails.boot.config.GrailsApplicationPostProcessor
import grails.core.GrailsApplication
import grails.core.GrailsApplicationLifeCycle
import grails.core.support.proxy.DefaultProxyHandler
import grails.plugins.GrailsPluginManager
import grails.spring.BeanBuilder
import grails.util.Holders
import groovy.transform.CompileDynamic
import io.micronaut.context.ApplicationContextConfiguration
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.core.order.Ordered
import io.micronaut.spring.context.factory.MicronautBeanFactoryConfiguration
import org.grails.plugins.IncludingPluginFilter
import org.grails.spring.context.support.GrailsPlaceholderConfigurer
import org.grails.spring.context.support.MapBasedSmartPropertyOverrideConfigurer
import org.grails.transaction.TransactionManagerPostProcessor
import org.springframework.beans.BeansException
import org.springframework.beans.MutablePropertyValues
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.beans.factory.config.ConstructorArgumentValues
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.AnnotationConfigUtils
import org.springframework.context.support.ConversionServiceFactoryBean
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.context.support.StaticMessageSource
import org.springframework.core.convert.ConversionService
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.Environment
import org.springframework.core.env.PropertyResolver
import org.springframework.util.ClassUtils

/**
 * Created by jameskleeh on 5/31/17.
 */
class GrailsApplicationBuilder {

    public static final boolean isServletApiPresent = ClassUtils.isPresent("javax.servlet.ServletContext", GrailsApplicationBuilder.classLoader)

    static final Set DEFAULT_INCLUDED_PLUGINS = ['core', 'eventBus'] as Set

    Closure doWithSpring
    Closure doWithConfig
    Set<String> includePlugins
    boolean loadExternalBeans
    boolean localOverride = false

    GrailsApplication grailsApplication
    Object servletContext

    GrailsApplicationBuilder build() {
        servletContext = createServletContext()
        ConfigurableApplicationContext mainContext = createMainContext(servletContext)

        if (isServletApiPresent) {
            // NOTE: The following dynamic class loading hack is temporary so the
            // compile time dependency on the servlet api can be removed from this
            // sub project.  This whole GrailsApplicationTestPlugin class will soon
            // be removed so rather than implement a real solution, this hack will
            // do for now to keep the build healthy.
            try {
                Class segads = Class.forName('org.grails.web.context.ServletEnvironmentGrailsApplicationDiscoveryStrategy')
                Holders.addApplicationDiscoveryStrategy(segads.newInstance(servletContext))
            } catch (Throwable t) {

            }
            try {
                Class gcu = Class.forName('org.grails.web.servlet.context.GrailsConfigUtils')
                gcu.configureServletContextAttributes(servletContext, grailsApplication, mainContext.getBean(GrailsPluginManager.BEAN_NAME, GrailsPluginManager), mainContext)
            } catch (Throwable t) {

            }
        }

        grailsApplication = mainContext.getBean('grailsApplication')

        if (!grailsApplication.isInitialised()) {
            grailsApplication.initialise()
        }
        this
    }

    protected Object createServletContext() {
        if (isServletApiPresent) {
            def context = ClassUtils.forName("org.springframework.mock.web.MockServletContext").newInstance()
            Holders.setServletContext(context)
            return context
        }
    }

    protected ConfigurableApplicationContext createMainContext(Object servletContext) {
        ConfigurableApplicationContext context

        if (isServletApiPresent && servletContext != null) {
            context = (ConfigurableApplicationContext) ClassUtils.forName("org.springframework.web.context.support.GenericWebApplicationContext").newInstance(servletContext);
        } else {
            context = (ConfigurableApplicationContext) ClassUtils.forName("org.springframework.context.support.GenericApplicationContext").newInstance()
        }

        ClassLoader applicationClassLoader = this.class.classLoader
        ConfigurableEnvironment configuredEnvironment = context.getEnvironment()
        ApplicationContextConfiguration micronautConfiguration = new ApplicationContextConfiguration() {
            @Override
            List<String> getEnvironments() {
                if (configuredEnvironment != null) {
                    return configuredEnvironment.getActiveProfiles().toList()
                } else {
                    return Collections.emptyList()
                }
            }

            @Override
            Optional<Boolean> getDeduceEnvironments() {
                return Optional.of(false)
            }

            @Override
            ClassLoader getClassLoader() {
                return applicationClassLoader
            }
        }

        ConfigurableBeanFactory beanFactory = context.getBeanFactory()
        List beanExcludes = []
        beanExcludes.add(ConversionService.class)
        beanExcludes.add(Environment.class)
        beanExcludes.add(PropertyResolver.class)
        beanExcludes.add(ConfigurableEnvironment.class)
        def objectMapper = io.micronaut.core.reflect.ClassUtils.forName("com.fasterxml.jackson.databind.ObjectMapper", context.getClassLoader()).orElse(null)
        if (objectMapper != null) {
            beanExcludes.add(objectMapper)
        }
        def micronautContext = new DefaultApplicationContext(micronautConfiguration);
        micronautContext
                .environment
                .addPropertySource("grails-config", [(MicronautBeanFactoryConfiguration.PREFIX + ".bean-excludes"): (Object) beanExcludes])
        micronautContext.start()
        ConfigurableApplicationContext parentContext = micronautContext.getBean(ConfigurableApplicationContext)
        ((DefaultListableBeanFactory) beanFactory)
                .setAllowBeanDefinitionOverriding(true);
        context.setParent(
                parentContext
        )
        context.addApplicationListener(new GrailsApp.MicronautShutdownListener(micronautContext))
        prepareContext(context, beanFactory)
        context.refresh()
        context.registerShutdownHook()

        context
    }

    protected void prepareContext(ConfigurableApplicationContext applicationContext, ConfigurableBeanFactory beanFactory) {
        registerGrailsAppPostProcessorBean(beanFactory)

        AnnotationConfigUtils.registerAnnotationConfigProcessors((BeanDefinitionRegistry) beanFactory)
        new ConfigDataApplicationContextInitializer().initialize(applicationContext)
    }

    void executeDoWithSpringCallback(GrailsApplication grailsApplication) {
        if (!doWithSpring) return
        defineBeans(grailsApplication, doWithSpring)
    }

    void defineBeans(Closure callable) {
        defineBeans(grailsApplication, callable)
    }

    void defineBeans(GrailsApplication grailsApplication, Closure callable) {
        def binding = new Binding()
        def bb = new BeanBuilder(null, null, grailsApplication.getClassLoader())
        binding.setVariable "application", grailsApplication
        bb.setBinding binding
        bb.beans(callable)
        bb.registerBeans((BeanDefinitionRegistry) grailsApplication.getMainContext())
    }

    void registerBeans(GrailsApplication grailsApplication) {

        defineBeans(grailsApplication) { ->
            conversionService(ConversionServiceFactoryBean)

            xmlns context: "http://www.springframework.org/schema/context"
            // adds AutowiredAnnotationBeanPostProcessor, CommonAnnotationBeanPostProcessor and others
            // see org.springframework.context.annotation.AnnotationConfigUtils.registerAnnotationConfigProcessors method
            context.'annotation-config'()

            proxyHandler(DefaultProxyHandler)
            messageSource(StaticMessageSource)
            transactionManagerAwarePostProcessor(TransactionManagerPostProcessor)
            grailsPlaceholderConfigurer(GrailsPlaceholderConfigurer, '${', grailsApplication.config.toProperties())
            mapBasedSmartPropertyOverrideConfigurer(MapBasedSmartPropertyOverrideConfigurer) {
                grailsApplication = grailsApplication
            }
        }
    }

    @CompileDynamic
    protected void registerGrailsAppPostProcessorBean(ConfigurableBeanFactory beanFactory) {

        GrailsApplication grailsApp

        Closure doWithSpringClosure = {
            registerBeans(grailsApp)
            executeDoWithSpringCallback(grailsApp)
        }

        Closure customizeGrailsApplicationClosure = { grailsApplication ->
            grailsApp = grailsApplication
            if (doWithConfig) {
                doWithConfig.call(grailsApplication.config)
                // reset flatConfig
                grailsApplication.configChanged()
            }
            Holders.config = grailsApplication.config
        }

        ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues()
        constructorArgumentValues.addIndexedArgumentValue(0, doWithSpringClosure)
        constructorArgumentValues.addIndexedArgumentValue(1, includePlugins ?: DEFAULT_INCLUDED_PLUGINS)

        MutablePropertyValues values = new MutablePropertyValues()
        values.add('localOverride', localOverride)
        values.add('loadExternalBeans', loadExternalBeans)
        values.add('customizeGrailsApplicationClosure', customizeGrailsApplicationClosure)

        RootBeanDefinition beandef = new RootBeanDefinition(TestRuntimeGrailsApplicationPostProcessor, constructorArgumentValues, values)
        beandef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE)
        beanFactory.registerBeanDefinition("grailsApplicationPostProcessor", beandef)
    }

    static class TestRuntimeGrailsApplicationPostProcessor extends GrailsApplicationPostProcessor {
        Closure customizeGrailsApplicationClosure
        Set includedPlugins
        boolean localOverride = false

        TestRuntimeGrailsApplicationPostProcessor(Closure doWithSpringClosure, Set includedPlugins) {
            super([doWithSpring: { -> doWithSpringClosure }] as GrailsApplicationLifeCycle, null, null)
            loadExternalBeans = false
            reloadingEnabled = false
            this.includedPlugins = includedPlugins
        }

        @Override
        protected void customizePluginManager(GrailsPluginManager grailsApplication) {
            pluginManager.pluginFilter = new IncludingPluginFilter(includedPlugins)
        }

        @Override
        protected void customizeGrailsApplication(GrailsApplication grailsApplication) {
            customizeGrailsApplicationClosure?.call(grailsApplication)
        }

        @Override
        void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
            super.postProcessBeanDefinitionRegistry(registry)
            PropertySourcesPlaceholderConfigurer propertySourcePlaceholderConfigurer  = (PropertySourcesPlaceholderConfigurer) grailsApplication.mainContext.getBean('grailsPlaceholderConfigurer')
            propertySourcePlaceholderConfigurer.order = Ordered.HIGHEST_PRECEDENCE
            propertySourcePlaceholderConfigurer.setLocalOverride(localOverride)
        }
    }
}

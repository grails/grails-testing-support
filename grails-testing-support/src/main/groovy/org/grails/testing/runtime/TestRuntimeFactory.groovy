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

package org.grails.testing.runtime

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.core.io.support.GrailsFactoriesLoader

/**
 * TestRuntimeFactory is a singleton that contains the TestPlugin registry and has methods 
 * for getting a TestRuntime instance with given features.
 * 
 * It will resolve the requested features and pick the minimum set of plugins 
 * that are required for covering the requested features.
 * 
 */
@CompileStatic
@Singleton
@Slf4j
class TestRuntimeFactory {
    private static Set<Class<? extends TestPlugin>> availablePluginClasses = new HashSet<>()

    static {
        def testPluginClasses = GrailsFactoriesLoader.loadFactoryClasses(TestPlugin)
        availablePluginClasses.addAll(testPluginClasses)
    }

    static TestRuntime getRuntimeForTestClass(Class<?> testClass) {
        Set<String> allFeatures = [] as Set
        Set<TestPluginUsage> allTestPluginUsages = [] as Set
        TestRuntimeSettings runtimeSettings = new TestRuntimeSettings(requiredFeatures: allFeatures, pluginUsages: allTestPluginUsages as List)
        TestRuntime runtime = TestRuntimeFactory.getInstance().findOrCreateRuntimeForTestClass(testClass, runtimeSettings)
        runtime
    }

    /**
     * 
     * Registers TestPlugin class to global static plugin registry.
     * This method can be used in static initialization blocks of a class. However it's 
     * recommended that the mixin class implements {@link org.grails.testing.runtime.TestPluginRegistrar} to register the
     * plugins that implement the features required by the mixin class.
     * 
     * @param pluginClass
     */
    static void addPluginClass(Class<? extends TestPlugin> pluginClass) {
        TestRuntimeFactory.getInstance().addTestPluginClass(pluginClass)
    }

    static void removePluginClass(Class<? extends TestPlugin> pluginClass) {
        TestRuntimeFactory.getInstance().removeTestPluginClass(pluginClass)
    }
    
    private TestRuntime findOrCreateRuntimeForTestClass(Class testClass, TestRuntimeSettings runtimeSettings) {
        TestRuntime runtime=activeRuntimes.get(testClass)
        if(runtime==null) {
            runtime = createRuntimeForSettings(runtimeSettings)
            activeRuntimes.put(testClass, runtime)
        } else {
            checkRuntimeFeatures(runtime, runtimeSettings.requiredFeatures)
        }
        runtime
    }
    
    private void checkRuntimeFeatures(TestRuntime runtime, Set<String> features) {
        if(features && !runtime.features.containsAll(features)) {
            Set<String> missingFeatures = [] as Set
            missingFeatures.addAll(features)
            missingFeatures.removeAll(runtime.features)
            throw new TestRuntimeFactoryException("Current shared runtime doesn't contain required feature:" + missingFeatures.join(',')) 
        }
    }
        
    private TestRuntime createRuntimeForSettings(TestRuntimeSettings runtimeSettings) {
        TestRuntime runtime
        if(runtimeSettings.requiredFeatures) {
            runtime = new TestRuntime(runtimeSettings.requiredFeatures, resolveFeaturesToPlugins(runtimeSettings))
        } else {
            // setup runtime with all available plugins
            Map<String, TestPlugin> featureToPlugin = resolvePlugins(runtimeSettings)
            Set<String> allFeatures = new LinkedHashSet(featureToPlugin.keySet())
            List<TestPlugin> allPlugins = resolveTransitiveDependencies(allFeatures, featureToPlugin)
            runtime = new TestRuntime(allFeatures, allPlugins)
        }
        return runtime
    }

    private List resolveFeaturesToPlugins(TestRuntimeSettings runtimeSettings) {
        Map<String, TestPlugin> featureToPlugin = resolvePlugins(runtimeSettings)
        List<String> requestActivationForFeatures = resolvePluginFeaturesToActivate(runtimeSettings, featureToPlugin)
        runtimeSettings.requiredFeatures.addAll(requestActivationForFeatures)
        List<TestPlugin> requiredPlugins = resolveTransitiveDependencies(runtimeSettings.requiredFeatures, featureToPlugin)
        return requiredPlugins
    }

    private List resolvePluginFeaturesToActivate(TestRuntimeSettings runtimeSettings, Map<String, TestPlugin> featureToPlugin) {
        List<String> requestActivationForFeatures = (List<String>)runtimeSettings.pluginUsages.inject([]) { List<String> accumulator, TestPluginUsage testPluginUsage ->
            if(testPluginUsage.requestActivation && !testPluginUsage.exclude) {
                testPluginUsage.pluginClasses.each { Class clazz ->
                    String[] features = featureToPlugin.values().find { TestPlugin instance ->
                        instance.getClass() == clazz
                    }?.getProvidedFeatures()
                    features && accumulator.addAll(features as List)
                }
            }
            accumulator
        }
        return requestActivationForFeatures
    }

    private List<TestPlugin> resolveTransitiveDependencies(Set<String> features, Map<String, TestPlugin> featureToPlugin) {
        // resolve required plugins
        List<TestPlugin> requiredPlugins = features.collect { String feature ->
            resolveFeature(feature, featureToPlugin)
        }

        // resolve transitive plugins
        List<TestPlugin> allPlugins = sortByOrdinal((List<TestPlugin>)requiredPlugins.inject([]) { List<TestPlugin> collector, TestPlugin plugin ->
            collector.add(plugin)
            collector.addAll(resolveRequiredPlugins(plugin, featureToPlugin))
            collector
        })

        // sort by dependency order (topological sort)
        sortPlugins(allPlugins, featureToPlugin)
    }

    private List<TestPlugin> resolveRequiredPlugins(TestPlugin plugin, Map<String, TestPlugin> featureToPlugin) {
        sortByOrdinal((List<TestPlugin>)plugin.requiredFeatures.collect{ String feature -> resolveFeature(feature, featureToPlugin) })
    }

    private TestPlugin resolveFeature(String feature, Map<String, TestPlugin> featureToPlugin) {
        TestPlugin plugin = featureToPlugin.get(feature)
        if(plugin == null) {
            throw new TestRuntimeFactoryException("No plugin available for feature ${feature}")
        }
        plugin
    }
    
    private List<TestPlugin> sortPlugins(List<TestPlugin> toSort, Map<String, TestPlugin> featureToPlugin) {
        /* http://en.wikipedia.org/wiki/Topological_sorting
         *
        * L ← Empty list that will contain the sorted nodes
         S ← Set of all nodes

        function visit(node n)
            if n has not been visited yet then
                mark n as visited
                for each node m with an edge from n to m do
                    visit(m)
                add n to L

        for each node n in S do
            visit(n)

         */
        List<TestPlugin> sortedPlugins = new ArrayList<TestPlugin>(toSort.size())
        Set<TestPlugin> visitedPlugins = new HashSet<TestPlugin>()

        for (TestPlugin plugin : toSort) {
            visitTopologicalSort(plugin, sortedPlugins, visitedPlugins, featureToPlugin)
        }

        return sortedPlugins
    }

    private void visitTopologicalSort(TestPlugin plugin, List<TestPlugin> sortedPlugins, Set<TestPlugin> visitedPlugins, Map<String, TestPlugin> featureToPlugin) {
        if(plugin != null && !visitedPlugins.contains(plugin)) {
            visitedPlugins.add(plugin)
            List<TestPlugin> dependenciesForPlugin = resolveRequiredPlugins(plugin, featureToPlugin)
            if(dependenciesForPlugin != null) {
                for(TestPlugin dependentPlugin : dependenciesForPlugin) {
                    visitTopologicalSort(dependentPlugin, sortedPlugins, visitedPlugins, featureToPlugin)
                }
            }
            sortedPlugins.add(plugin)
        }
    }

    // maps feature to plugin with lowest ordinal (highest priority)
    private Map<String, TestPlugin> resolvePlugins(TestRuntimeSettings runtimeSettings) {
        Map<String, List<TestPlugin>> featureToPlugins = [:]
        
        Set<Class<? extends TestPlugin>> pluginClassesToUse = resolvePluginClassesToUse(runtimeSettings)
        
        List<TestPlugin> availablePlugins = instantiatePlugins(pluginClassesToUse)
        for(TestPlugin plugin : availablePlugins) {
            for(String feature : plugin.getProvidedFeatures()) {
                def pluginList = featureToPlugins.get(feature)
                if(pluginList==null) {
                    pluginList = []
                    featureToPlugins.put(feature, pluginList)
                }
                pluginList.add(plugin)
            }
        }
        // pick one plugin for feature implementation
        (Map<String, TestPlugin>)featureToPlugins.collectEntries { String feature, List<TestPlugin> pluginList ->
            // sort plugins with lowest ordinal (highest priority) and pick the first one
            if(pluginList.size() > 1) {
                pluginList = sortByOrdinal(pluginList)
            }
            [feature, pluginList[0]]
        }
    }

    protected List<TestPlugin> instantiatePlugins(Set<Class<? extends TestPlugin>> pluginClassesToUse) {
        List<TestPlugin> plugins = []
        for(Class<? extends TestPlugin> testPlugin in pluginClassesToUse) {
            try {
                plugins.add(
                        testPlugin.newInstance()
                )

            } catch (Throwable e) {
                log.debug("Error creating test plugin: ${e.message} due to missing dependencies. Ignoring.", e)
            }
        }
        return plugins
    }

    private Set<Class<? extends TestPlugin>> resolvePluginClassesToUse(TestRuntimeSettings runtimeSettings) {
        Set<Class<? extends TestPlugin>> pluginClassesToUse = [] as Set
        pluginClassesToUse.addAll(availablePluginClasses)
        runtimeSettings.pluginUsages.each { TestPluginUsage testPluginUsage ->
            testPluginUsage.exclude ? pluginClassesToUse.removeAll(testPluginUsage.pluginClasses) : pluginClassesToUse.addAll(testPluginUsage.pluginClasses)
        }
        return pluginClassesToUse
    }

    private List<TestPlugin> sortByOrdinal(List<TestPlugin> pluginList) {
        // remove duplicates
        def pluginSet = [] as Set
        pluginSet.addAll(pluginList)
        def newPluginList = new ArrayList(pluginSet)
        // sort by ordinal, reverse order (highest value comes first)
        newPluginList.sort(false) { TestPlugin a, TestPlugin b ->
            b.ordinal <=> a.ordinal
        }
    }

    private void addTestPluginClass(Class<? extends TestPlugin> pluginClass) {
        availablePluginClasses.add(pluginClass)
    }

    private void removeTestPluginClass(Class<? extends TestPlugin> pluginClass) {
        availablePluginClasses.remove(pluginClass)
    }
    
    private Map<Class, TestRuntime> activeRuntimes=new HashMap<Class, TestRuntime>()
    
    static void removeRuntime(TestRuntime runtime) {
        TestRuntimeFactory.getInstance().removeTestRuntime(runtime)
    }

    private void removeTestRuntime(TestRuntime runtime) {
        removeRuntimeFromMap(activeRuntimes, runtime)
    }

    private removeRuntimeFromMap(Map<Class, TestRuntime> cachedRuntimeMap, TestRuntime runtime) {
        for(Iterator iterator=cachedRuntimeMap.entrySet().iterator();iterator.hasNext();) {
            Map.Entry entry=iterator.next()
            if(entry.value==runtime) {
                iterator.remove()
                return
            }
        }
    }
}

@CompileStatic
class TestRuntimeFactoryException extends RuntimeException {
    public TestRuntimeFactoryException(String message) {
        super(message)
    }
}
package org.grails.testing.compiler

import grails.compiler.ast.GrailsArtefactClassInjector
import grails.test.runtime.TestRuntimeJunitAdapter
import grails.util.GrailsNameUtils
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.*
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.grails.compiler.injection.GrailsASTUtils
import org.grails.compiler.injection.test.TestForTransformation
import org.grails.compiler.logging.LoggingTransformer
import org.grails.testing.GrailsUnitTest
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.rules.TestRule
import org.spockframework.runtime.model.FieldMetadata
import org.springframework.context.ApplicationContext
import spock.lang.Shared

import java.lang.reflect.Modifier

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class GrailsUnitTestTransformation implements ASTTransformation {
    private static
    final String GROOVY_TEST_CASE_CLASS_NAME = "groovy.util.GroovyTestCase"
    private static final Token ASSIGN = Token.newSymbol("=", -1, -1)
    protected static
    final List<String> artefactTypes = ['Controller', 'Service']
    public static
    final ClassNode BEFORE_CLASS_NODE = new ClassNode(Before.class)
    public static
    final AnnotationNode BEFORE_ANNOTATION = new AnnotationNode(BEFORE_CLASS_NODE)
    private static final String RULE_FIELD_NAME_BASE = '$testRuntime'
    private static
    final String JUNIT_ADAPTER_FIELD_NAME = RULE_FIELD_NAME_BASE + "JunitAdapter"
    private static
    final String JUNIT3_RULE_SETUP_TEARDOWN_APPLIED_KEY = "JUNIT3_RULE_SETUP_TEARDOWN_APPLIED_KEY"
    public static final String SET_UP_METHOD = "setUp"
    public static final String TEAR_DOWN_METHOD = "tearDown"
    private static final String JUNIT3_CLASS = "junit.framework.TestCase"
    public static final String SPEC_CLASS = "spock.lang.Specification"

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {

        ClassNode grailsUnitTestClassNode = ClassHelper.make(GrailsUnitTest)
        ModuleNode ast = source.getAST()

        for (ClassNode classNode : ast.classes) {
            if (classNode.implementsInterface(grailsUnitTestClassNode)) {
                ClassNode[] interfaces = classNode.getInterfaces()
                for (ClassNode implementedInterface : interfaces) {
                    if (implementedInterface.implementsInterface(grailsUnitTestClassNode)) {
                        GenericsType[] genericTypes = implementedInterface.getGenericsTypes()
                        ClassNode artefactClassNode = genericTypes[0].type
                        transformTestClass classNode, artefactClassNode
                    }
                }
            }
        }
    }

    /**
     * Main entry point for the calling the TestForTransformation programmatically.
     *
     * @param classNode The class node that represents th test
     * @param ce The class expression that represents the class to test
     */
    public static void transformTestClass(ClassNode classNode, ClassNode ce) {

        autoAnnotateSetupTeardown(classNode)
        boolean isJunit3Test = isJunit3Test(classNode)

        // make sure the 'log' property is not the one from GroovyTestCase
        FieldNode log = classNode.getField("log")
        if (log == null || log.getDeclaringClass().name == GROOVY_TEST_CASE_CLASS_NAME) {
            LoggingTransformer.addLogField(classNode, classNode.name)
        }
        boolean isSpockTest = isSpockTest(classNode)

        boolean isJunit4 = !isSpockTest && !isJunit3Test

        if (isJunit4 || isJunit3Test || isSpockTest) {
            final MethodNode methodToAdd = weaveMock(classNode, ce)
            if (methodToAdd != null && isJunit3Test) {
                addMethodCallsToMethod(classNode, SET_UP_METHOD, Arrays.asList(methodToAdd))
            }
        }
    }

    protected static MethodNode weaveMock(ClassNode testClassNode, ClassNode artifactClassNode) {

        ClassNode testTarget = artifactClassNode
        String className = testTarget.getName()
        for (String artefactType : artefactTypes) {
            if (className.endsWith(artefactType)) {
                configureJunitHandler(testClassNode)
                return addClassUnderTestMethod(testClassNode, artifactClassNode, artefactType)
            }
        }
        return null
    }

    protected
    static MethodNode addClassUnderTestMethod(ClassNode testClassNode, ClassNode artefactTypeClassNode, String type) {

        String methodName = "setup" + type + "UnderTest"
        String fieldName = GrailsNameUtils.getPropertyName(type)
        String getterName = 'getCollaboratorInstance'
        fieldName = '$' + fieldName

        if (testClassNode.getField(fieldName) == null) {
            testClassNode.addField(fieldName, Modifier.PRIVATE, artefactTypeClassNode, null)
        }

        MethodNode methodNode = testClassNode.getDeclaredMethod(methodName, GrailsArtefactClassInjector.ZERO_PARAMETERS)

        VariableExpression fieldExpression = new VariableExpression(fieldName, artefactTypeClassNode)
        if (methodNode == null) {
            BlockStatement setupMethodBody = new BlockStatement()
            addMockCollaborator(type, artefactTypeClassNode, setupMethodBody)

            methodNode = new MethodNode(methodName, Modifier.PUBLIC, ClassHelper.VOID_TYPE, GrailsArtefactClassInjector.ZERO_PARAMETERS, null, setupMethodBody)
            methodNode.addAnnotation(BEFORE_ANNOTATION)

            testClassNode.addMethod(methodNode)
            GrailsASTUtils.addCompileStaticAnnotation(methodNode)
        }

        MethodNode getter = testClassNode.getDeclaredMethod(getterName, GrailsArtefactClassInjector.ZERO_PARAMETERS)
        if (getter == null) {
            BlockStatement getterBody = new BlockStatement()
            getter = new MethodNode(getterName, Modifier.PUBLIC, artefactTypeClassNode.getPlainNodeReference(), GrailsArtefactClassInjector.ZERO_PARAMETERS, null, getterBody)

            BinaryExpression testTargetAssignment = new BinaryExpression(fieldExpression, ASSIGN, new ConstructorCallExpression(artefactTypeClassNode, GrailsArtefactClassInjector.ZERO_ARGS))

            IfStatement autowiringIfStatement = getAutowiringIfStatement(artefactTypeClassNode, fieldExpression, testTargetAssignment)
            getterBody.addStatement(autowiringIfStatement)

            getterBody.addStatement(new ReturnStatement(fieldExpression))
            testClassNode.addMethod(getter)
            GrailsASTUtils.addCompileStaticAnnotation(getter)
        }

        return methodNode
    }

    protected
    static IfStatement getAutowiringIfStatement(ClassNode targetClass, VariableExpression fieldExpression, BinaryExpression testTargetAssignment) {
        VariableExpression appCtxVar = new VariableExpression("applicationContext", ClassHelper.make(ApplicationContext.class))

        BooleanExpression applicationContextCheck = new BooleanExpression(
                new BinaryExpression(
                        new BinaryExpression(fieldExpression, GrailsASTUtils.EQUALS_OPERATOR, GrailsASTUtils.NULL_EXPRESSION),
                        Token.newSymbol("&&", 0, 0),
                        new BinaryExpression(appCtxVar, GrailsASTUtils.NOT_EQUALS_OPERATOR, GrailsASTUtils.NULL_EXPRESSION)))
        BlockStatement performAutowireBlock = new BlockStatement()
        ArgumentListExpression arguments = new ArgumentListExpression()
        arguments.addExpression(fieldExpression)
        arguments.addExpression(new ConstantExpression(1))
        arguments.addExpression(new ConstantExpression(false))
        BlockStatement assignFromApplicationContext = new BlockStatement()
        ArgumentListExpression argWithClassName = new ArgumentListExpression()
        MethodCallExpression getClassNameMethodCall = new MethodCallExpression(new ClassExpression(targetClass), "getName", new ArgumentListExpression())
        argWithClassName.addExpression(getClassNameMethodCall)

        assignFromApplicationContext.addStatement(new ExpressionStatement(new BinaryExpression(fieldExpression, ASSIGN, new MethodCallExpression(appCtxVar, "getBean", argWithClassName))))
        BlockStatement elseBlock = new BlockStatement()
        elseBlock.addStatement(new ExpressionStatement(testTargetAssignment))
        performAutowireBlock.addStatement(new IfStatement(new BooleanExpression(new MethodCallExpression(appCtxVar, "containsBean", argWithClassName)), assignFromApplicationContext, elseBlock))
        performAutowireBlock.addStatement(new ExpressionStatement(new MethodCallExpression(new PropertyExpression(appCtxVar, "autowireCapableBeanFactory"), "autowireBeanProperties", arguments)))
        return new IfStatement(applicationContextCheck, performAutowireBlock, new BlockStatement())
    }

    protected
    static void addMockCollaborator(String mockType, ClassNode targetClass, BlockStatement methodBody) {
        ArgumentListExpression args = new ArgumentListExpression()
        args.addExpression(new ClassExpression(targetClass))
        methodBody.getStatements().add(0, new ExpressionStatement(new MethodCallExpression(new VariableExpression("this"), "mock" + mockType, args)))
    }

    public static void configureJunitHandler(ClassNode classNode) {
        Junit3TestFixtureMethodHandler junit3MethodHandler = isJunit3Test(classNode) ? new Junit3TestFixtureMethodHandler(classNode) : null

        addJunitRuleFields classNode

        if (junit3MethodHandler != null) {
            junit3MethodHandler.postProcessClassNode()
        }
    }

    private static class Junit3TestFixtureMethodHandler {
        ClassNode classNode
        List<MethodNode> beforeMethods = new ArrayList<MethodNode>()
        List<MethodNode> afterMethods = new ArrayList<MethodNode>()
        boolean hasExistingSetUp
        boolean hasExistingTearDown

        public Junit3TestFixtureMethodHandler(ClassNode classNode) {
            this.classNode = classNode
            hasExistingSetUp = classNode.hasDeclaredMethod(SET_UP_METHOD, Parameter.EMPTY_ARRAY)
            hasExistingTearDown = classNode.hasDeclaredMethod(TEAR_DOWN_METHOD, Parameter.EMPTY_ARRAY)
        }

        public void postProcessClassNode() {
            addMethodCallsToMethod(classNode, SET_UP_METHOD, beforeMethods)
            addMethodCallsToMethod(classNode, TEAR_DOWN_METHOD, afterMethods)
            handleTestRuntimeJunitSetUpAndTearDownCalls()
        }

        private void handleTestRuntimeJunitSetUpAndTearDownCalls() {
            FieldNode junitAdapterFieldNode = classNode.getDeclaredField(JUNIT_ADAPTER_FIELD_NAME)
            if (junitAdapterFieldNode == null) {
                return
            }

            // add rule calls to junit setup/teardown only once, there might be several test mixins applied for the same class
            if (classNode.redirect().getNodeMetaData(JUNIT3_RULE_SETUP_TEARDOWN_APPLIED_KEY) != Boolean.TRUE) {
                BlockStatement setUpMethodBody = getOrCreateNoArgsMethodBody(classNode, SET_UP_METHOD)
                if (!hasExistingSetUp) {
                    setUpMethodBody.getStatements().add(0, new ExpressionStatement(new MethodCallExpression(new VariableExpression("super"), SET_UP_METHOD, GrailsArtefactClassInjector.ZERO_ARGS)))
                }
                BlockStatement tearDownMethodBody = getOrCreateNoArgsMethodBody(classNode, TEAR_DOWN_METHOD)
                setUpMethodBody.getStatements().add(1, new ExpressionStatement(new MethodCallExpression(new FieldExpression(junitAdapterFieldNode), SET_UP_METHOD, new VariableExpression("this"))))
                tearDownMethodBody.addStatement(new ExpressionStatement(new MethodCallExpression(new FieldExpression(junitAdapterFieldNode), TEAR_DOWN_METHOD, new VariableExpression("this"))))
                if (!hasExistingTearDown) {
                    tearDownMethodBody.addStatement(new ExpressionStatement(new MethodCallExpression(new VariableExpression("super"), TEAR_DOWN_METHOD, GrailsArtefactClassInjector.ZERO_ARGS)))
                }
                classNode.redirect().setNodeMetaData(JUNIT3_RULE_SETUP_TEARDOWN_APPLIED_KEY, Boolean.TRUE)
            }
        }
    }

    static protected void addMethodCallsToMethod(ClassNode classNode, String name, List<MethodNode> methods) {
        if (methods != null && !methods.isEmpty()) {
            BlockStatement setupMethodBody = getOrCreateNoArgsMethodBody(classNode, name)
            for (MethodNode beforeMethod : methods) {
                setupMethodBody.addStatement(new ExpressionStatement(new MethodCallExpression(new VariableExpression("this"), beforeMethod.getName(), GrailsArtefactClassInjector.ZERO_ARGS)))
            }
        }
    }

    static protected BlockStatement getOrCreateNoArgsMethodBody(ClassNode classNode, String name) {
        MethodNode setupMethod = classNode.getMethod(name, GrailsArtefactClassInjector.ZERO_PARAMETERS)
        return getOrCreateMethodBody(classNode, setupMethod, name)
    }

    protected static void autoAnnotateSetupTeardown(ClassNode classNode) {
        MethodNode setupMethod = classNode.getDeclaredMethod(SET_UP_METHOD, GrailsArtefactClassInjector.ZERO_PARAMETERS)
        if (setupMethod != null && setupMethod.getAnnotations(TestForTransformation.BEFORE_CLASS_NODE).size() == 0) {
            setupMethod.addAnnotation(TestForTransformation.BEFORE_ANNOTATION)
        }

        MethodNode tearDown = classNode.getDeclaredMethod(TEAR_DOWN_METHOD, GrailsArtefactClassInjector.ZERO_PARAMETERS)
        if (tearDown != null && tearDown.getAnnotations(TestForTransformation.AFTER_CLASS_NODE).size() == 0) {
            tearDown.addAnnotation(TestForTransformation.AFTER_ANNOTATION)
        }
    }

    static
    protected BlockStatement getOrCreateMethodBody(ClassNode classNode, MethodNode methodNode, String name) {
        BlockStatement methodBody
        if (!(methodNode.getDeclaringClass() == classNode)) {
            methodBody = new BlockStatement()
            methodNode = new MethodNode(name, Modifier.PUBLIC, methodNode.getReturnType(), GrailsArtefactClassInjector.ZERO_PARAMETERS, null, methodBody)
            classNode.addMethod(methodNode)
        } else {
            final Statement setupMethodBody = methodNode.getCode()
            if (!(setupMethodBody instanceof BlockStatement)) {
                methodBody = new BlockStatement()
                if (setupMethodBody != null) {
                    if (!(setupMethodBody instanceof ReturnStatement)) {
                        methodBody.addStatement(setupMethodBody)
                    }
                }
                methodNode.setCode(methodBody)
            } else {
                methodBody = (BlockStatement) setupMethodBody
            }
        }
        return methodBody
    }

    public static boolean isJunit3Test(ClassNode classNode) {
        return GrailsASTUtils.isSubclassOf(classNode, JUNIT3_CLASS)
    }

    public static boolean isSpockTest(ClassNode classNode) {
        return GrailsASTUtils.isSubclassOf(classNode, SPEC_CLASS)
    }

    protected static void addJunitRuleFields(ClassNode classNode) {
        if (classNode.getField(JUNIT_ADAPTER_FIELD_NAME) != null) {
            return
        }
        ClassNode junitAdapterType = ClassHelper.make(TestRuntimeJunitAdapter.class)
        FieldNode junitAdapterFieldNode = classNode.addField(JUNIT_ADAPTER_FIELD_NAME, Modifier.STATIC, junitAdapterType, new ConstructorCallExpression(junitAdapterType, MethodCallExpression.NO_ARGUMENTS))
        boolean spockTest = isSpockTest(classNode)
        FieldNode staticRuleFieldNode = classNode.addField(RULE_FIELD_NAME_BASE + "StaticClassRule", Modifier.PRIVATE | Modifier.STATIC, ClassHelper.make(TestRule.class), new MethodCallExpression(new FieldExpression(junitAdapterFieldNode), "newClassRule", new ClassExpression(classNode)))
        AnnotationNode classRuleAnnotation = new AnnotationNode(ClassHelper.make(ClassRule.class))
        if (spockTest) {
            // @ClassRule must be added to @Shared field in spock
            FieldNode spockSharedRuleFieldNode = classNode.addField(RULE_FIELD_NAME_BASE + "SharedClassRule", Modifier.PUBLIC, ClassHelper.make(TestRule.class), new FieldExpression(staticRuleFieldNode))
            spockSharedRuleFieldNode.addAnnotation(classRuleAnnotation)
            spockSharedRuleFieldNode.addAnnotation(new AnnotationNode(ClassHelper.make(Shared.class)))
            addSpockFieldMetadata(spockSharedRuleFieldNode, 0)
        } else {
            staticRuleFieldNode.setModifiers(Modifier.PUBLIC | Modifier.STATIC)
            staticRuleFieldNode.addAnnotation(classRuleAnnotation)
        }

        FieldNode ruleFieldNode = classNode.addField(RULE_FIELD_NAME_BASE + "Rule", Modifier.PUBLIC, ClassHelper.make(TestRule.class), new MethodCallExpression(new FieldExpression(junitAdapterFieldNode), "newRule", new VariableExpression("this")))
        ruleFieldNode.addAnnotation(new AnnotationNode(ClassHelper.make(Rule.class)))
        if (spockTest) {
            addSpockFieldMetadata(ruleFieldNode, 0)
        }
    }

    protected static void addSpockFieldMetadata(FieldNode field, int ordinal) {
        AnnotationNode ann = new AnnotationNode(ClassHelper.make(FieldMetadata.class))
        ann.setMember(FieldMetadata.NAME, new ConstantExpression(field.getName()))
        ann.setMember(FieldMetadata.ORDINAL, new ConstantExpression(ordinal))
        ann.setMember(FieldMetadata.LINE, new ConstantExpression(field.getLineNumber()))
        field.addAnnotation(ann)
    }
}

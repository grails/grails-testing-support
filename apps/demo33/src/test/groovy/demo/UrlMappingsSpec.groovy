package demo

// tag::setup[]
import grails.testing.web.UrlMappingsUnitTest
import spock.lang.Specification

class UrlMappingsSpec extends Specification implements UrlMappingsUnitTest<UrlMappings> {

    void setup() {
        mockController(TestController)
    }
// end::setup[]

    // tag::controller[]
    void "test controller"() {
        expect:
        verifyController("test")

        when: "Using the assert syntax"
        assertController("test")

        then:
        noExceptionThrown()
    }
    // end::controller[]

    // tag::action[]
    void "test action"() {
        expect:
        verifyAction("test", "renderText")

        when: "Using the assert syntax"
        assertAction("test", "renderText")

        then:
        noExceptionThrown()
    }
    // end::action[]

    // tag::view[]
    void "test view"() {
        expect:
        verifyView("test", "foo")

        when: "Using the assert syntax"
        assertView("test", "foo")

        then:
        noExceptionThrown()
    }
    // end::view[]

    // tag::forward[]
    void "test forward mappings"() {
        expect:
        verifyForwardUrlMapping("/test/renderText", controller: 'test', action: 'renderText')
        verifyForwardUrlMapping("/test/renderView", controller: 'test', action: 'renderView')
        verifyForwardUrlMapping("/test/renderState/123", controller: 'test', action: 'renderState') {
            id = '123'
        }
        verifyForwardUrlMapping("/", view: 'index')
        verifyForwardUrlMapping(500, view: 'error')
        verifyForwardUrlMapping(404, view: 'notFound')

        when: "Using the assert syntax"
        assertForwardUrlMapping("/test/renderText", controller: 'test', action: 'renderText')
        assertForwardUrlMapping("/test/renderView", controller: 'test', action: 'renderView')
        assertForwardUrlMapping("/test/renderState/123", controller: 'test', action: 'renderState') {
            id = 123
        }
        assertForwardUrlMapping("/", view: 'index')
        assertForwardUrlMapping(500, view: 'error')
        assertForwardUrlMapping(404, view: 'notFound')
        
        then:
        noExceptionThrown()
    }
    // end::forward[]

    // tag::reverse[]
    void "test reverse mappings"() {
        expect:
        verifyReverseUrlMapping("/test/renderText", controller: 'test', action: 'renderText')
        verifyReverseUrlMapping("/test/renderView", controller: 'test', action: 'renderView')
        verifyReverseUrlMapping("/test/renderState/123?foo=bar", controller: 'test', action: 'renderState') {
            id = 123
            foo = 'bar'
        }
        verifyReverseUrlMapping("/", view: 'index')
        
        when: "Using the assert syntax"
        assertReverseUrlMapping("/test/renderText", controller: 'test', action: 'renderText')
        assertReverseUrlMapping("/test/renderView", controller: 'test', action: 'renderView')
        assertReverseUrlMapping("/test/renderState/123?foo=bar", controller: 'test', action: 'renderState') {
            id = 123
            foo = 'bar'
        }
        assertReverseUrlMapping("/", view: 'index')
        
        then:
        noExceptionThrown()
    }
    // end::reverse[]

    // tag::combined[]
    void "test forward and reverse mappings"() {
        expect:
        verifyUrlMapping("/test/renderText", controller: 'test', action: 'renderText')
        verifyUrlMapping("/test/renderView", controller: 'test', action: 'renderView')
        verifyUrlMapping("/test/renderState/123", controller: 'test', action: 'renderState') {
            id = 123
        }
        verifyUrlMapping("/", view: 'index')

        when: "Using the assert syntax"
        assertUrlMapping("/test/renderText", controller: 'test', action: 'renderText')
        assertUrlMapping("/test/renderView", controller: 'test', action: 'renderView')
        assertUrlMapping("/test/renderState/123", controller: 'test', action: 'renderState') {
            id = 123
        }
        assertUrlMapping("/", view: 'index')
        
        then:
        noExceptionThrown()
    }
    // end::combined[]
    
    // tag::httpmethods[]
    void "test url mappings for different http methods"() {
        when: "the http method is GET, /api/route should map to ApiController.index()"
        request.method = "GET"
        assertUrlMapping("/api/route", controller: "api", action: "index", method: "GET")
        then: "expect no exception"
        noExceptionThrown()
        
        when: "the http method is POST, /api/route should map to ApiController.save()"
        request.method = "POST"
        assertUrlMapping("/api/route", controller: "api", action: "save", method: "POST")
        then: "expect no exception"
        noExceptionThrown()
    }
    // end::httpmethods[]
}

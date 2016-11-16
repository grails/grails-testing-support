package demo

// tag::basic_declaration[]
class DemoController {

// end::basic_declaration[]
    static allowedMethods = [clearDatabase: 'DELETE']

    // tag::render_hello[]
    def hello() {
        render 'Hello, World!'
    }

    // end::render_hello[]
    def clearDatabase() {
        render 'Success'
    }

    def invokeTagWhichInvokesTag() {
        response.writer << one.sayHello()
    }
// tag::basic_declaration[]
}
// end::basic_declaration[]

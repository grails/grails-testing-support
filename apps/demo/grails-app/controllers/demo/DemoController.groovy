package demo

class DemoController {

    static allowedMethods = [clearDatabase: 'DELETE']

    def hello() {
        render 'Hello, World!'
    }

    def clearDatabase() {
        render 'Success'
    }
}

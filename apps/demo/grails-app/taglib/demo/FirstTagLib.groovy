package demo

// tag::basic_declaration[]
class FirstTagLib {
    static defaultEncodeAs = [taglib:'html']

    static namespace = 'one'

    def sayHello = { attrs ->
        // this is invoking a tag from another tag library
        out << two.sayHello()
    }
}
// end::basic_declaration[]


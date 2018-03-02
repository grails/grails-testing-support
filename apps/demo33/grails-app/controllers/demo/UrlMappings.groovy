package demo

class UrlMappings {

    static mappings = {
        "/api/render" (method: "GET", controller: "test", action: "renderText")
        "/api/render" (method: "POST", controller: "test", action: "renderView")
        
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

        "/"(view:"/index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}

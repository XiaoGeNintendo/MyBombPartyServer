package top.hhs.xgn.mybombparty.server

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Application.configureRouting() {
    install(CORS){
        anyHost()
    }

    routing {
        get("/") {
            call.respondText("My Bomb Party Server has been deployed successfully! Current Accepting Client Version: ${MainData.requireClientVersion.joinToString(",")}")
        }
        get("/rooms"){
            call.respondText(Json.encodeToString(MainData.rooms))
        }
        get("/langs"){
            call.respondText(Json.encodeToString(MainData.dicts.keys))
        }
        get("/segments"){
            call.respondText(Json.encodeToString(MainData.segments.keys))
        }

        // Static plugin. Try to access `/static/index.html`
        static("/static") {
            resources("static")
        }
    }
}

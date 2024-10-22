package top.hhs.xgn.mybombparty.server

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("My Bomb Party Server has been deployed successfully! Current Accepting Client Version: ${MainData.requireClientVersion.joinToString(",")}")
        }
        get("/rooms"){
            call.respondText(Json.encodeToString(MainData.rooms))
        }
        // Static plugin. Try to access `/static/index.html`
        static("/static") {
            resources("static")
        }
    }
}

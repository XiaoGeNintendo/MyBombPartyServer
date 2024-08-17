package top.hhs.xgn.mybombparty.server

import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json
import top.hhs.xgn.mybombparty.data.GameRoom
import top.hhs.xgn.mybombparty.data.GameRoomPreview
import top.hhs.xgn.mybombparty.data.GameState
import top.hhs.xgn.mybombparty.data.Player
import top.hhs.xgn.mybombparty.hack.checkValid
import top.hhs.xgn.mybombparty.hack.receiveString
import java.time.Duration
import java.util.*

fun Application.configureSockets() {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {

        webSocket("/rooms") {
            //first check client version
            if(receiveString()!=MainData.requireClientVersion){
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT,"Invalid Client Version"))
                return@webSocket
            }
            for((k,room) in MainData.rooms){
                send(k)
                sendSerialized(room.toPreview())
            }

            close(CloseReason(CloseReason.Codes.NORMAL, "OK"))
        }

        webSocket("/createRoom"){
            //first check client version
            if(receiveString()!=MainData.requireClientVersion){
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT,"Invalid Client Version"))
                return@webSocket
            }
            try {
                val room = receiveDeserialized<GameRoomPreview>()
                if(!room.name.checkValid()){
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT,"Invalid name"))
                    return@webSocket
                }
                if(room.lang !in MainData.dicts){
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT,"Bad language"))
                    return@webSocket
                }
                if(room.segments !in MainData.segments){
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT,"Bad segment"))
                    return@webSocket
                }

                val newID = UUID.randomUUID().toString()
                if (newID in MainData.rooms) {
                    close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "Bad ID"))
                } else {
                    MainData.rooms[newID] = room.toGameRoom()
                    close(CloseReason(CloseReason.Codes.NORMAL, "OK"))
                }
            }catch(e:Exception){
                System.err.println("Cannot create room:")
                e.printStackTrace()
            }
        }

        webSocket("/join/{id}") {
            if(receiveString()!=MainData.requireClientVersion){
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT,"Invalid Client Version"))
                return@webSocket
            }
            val roomID=call.parameters["id"]
            if(roomID !in MainData.rooms){
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT,"No such room"))
                return@webSocket
            }

            val userName=receiveString()

            if(!userName.checkValid()){
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT,"Bad username"))
                return@webSocket
            }

            val room= MainData.rooms[roomID]!!

            sendSerialized(room) //send current room first

            if(room.state==GameState.BEFORE_START || room.players.any{it.name==userName}){

                var newPlayer=!room.players.any{it.name==userName}

                //register as player
                val success=room.connect(userName,this)

                if(newPlayer){
                    room.broadcast("new_player $userName")
                }else{
                    room.broadcast("connect $userName")
                }

                if(!success){
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT,"Player already exists"))
                    return@webSocket
                }

                try{
                    while(true){
                        val msg=receiveString().trim()
                        if(!msg.checkValid(1000)){
                            //directly reject
                            continue
                        }
                        if(msg=="start"){
                            if(room.isAdmin(userName) && room.state!=GameState.RUNNING && room.players.size>=2){
                                room.start()
                                println("Room $roomID started game!")
                            }else{
                                println("Could not start game of $roomID ${room.isAdmin(userName)} ${room.state==GameState.BEFORE_START} ${room.players.size>=2}")
                            }
                        }
                        if(msg=="closeRoom" && room.isAdmin(userName)){
                            //close room
                            room.close()
                            MainData.rooms.remove(roomID)
                            println("Room $roomID closed!")
                            break
                        }
                        room.processIncomingMessage(userName,msg)
                    }
                }catch(e:Exception) {
                    println("Player connection ended due to: $e")
                }finally {
                    //check not closed
                    if(room.players.size>=1) {
                        room.broadcast("disconnect $userName")
                        room.players.first { it.name == userName }.apply {
                            session=null
                            online=false
                        }
                    }
                }
            }else{
                //register as spectator
                val pl=Player(userName,-1).also { it.session=this }
                room.spectators.add(pl)

                room.broadcast("new_spectator $userName")
                try{
                    while(true){
                        receiveString()
                    }
                }catch(e:Exception){
                    println("Spectator connection ended due to: $e")
                }finally {

                    pl.session=null
                }
            }
        }
    }
}

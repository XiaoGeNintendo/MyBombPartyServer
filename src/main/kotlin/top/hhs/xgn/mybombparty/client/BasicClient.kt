package top.hhs.xgn.mybombparty.client

import kotlinx.coroutines.runBlocking
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import top.hhs.xgn.mybombparty.data.GameRoom
import top.hhs.xgn.mybombparty.hack.receiveString
import java.lang.System.exit
import java.util.*
import kotlin.system.exitProcess

lateinit var sc: Scanner
lateinit var client:HttpClient
var host="localhost"
var port=8058
var username="ZJS"

val clientVersion="4"

suspend fun roomPhase(){

    client.webSocket(HttpMethod.Get,host=host, port = port, path="/rooms"){
        send(clientVersion)
        println("All rooms:")
        for(frame in incoming){
            if(frame is Frame.Text){
                println(frame.readText())
            }
        }
    }

    println("Create(1) or join(2) or refresh(3)?")
    val choice=sc.nextInt()
    if(choice==1){

        println("Room name:")
//        val name=sc.next()
        val name="zjs"
        println("Language:")

//        val lang=sc.next()
        val lang="en"
        println("Segments:")
//        val seg=sc.next()
        val seg="en_easy"

        client.webSocket(HttpMethod.Get,host=host,port=port,path="/createRoom"){
            send(clientVersion)
            sendSerialized(GameRoom(name,lang,seg,100,12,3,2))
        }

        roomPhase()
    }else if(choice==2){

        println("Room ID:")
        val id=sc.next()

        client.webSocket(HttpMethod.Get, host=host,port=port,path="/join/$id"){
            send(clientVersion)
            send(username)

            val self=this
            launch{
                for(frame in self.incoming){
                    if(frame is Frame.Text){
                        println(frame.readText())
                    }
                }
                println("Incoming ended")
                exitProcess(0)
            }

            try {
                while (true) {
                    val line=sc.nextLine()
                    send(line)
                }
            }catch(e: Exception){
                //oops
                e.printStackTrace()
                println("Connection aborted.")
            }
        }
    }else if(choice==3){
        //refresh
        roomPhase()
    }
}
fun main() = runBlocking {
    client = HttpClient(CIO) {
        install(WebSockets){
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
    }

    sc=Scanner(System.`in`)
    println("Enter Username")
    username=sc.next()

    roomPhase()

    client.close()
}
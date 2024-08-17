package top.hhs.xgn.mybombparty.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import top.hhs.xgn.mybombparty.data.GameState

fun main(args: Array<String>) {

    //load dictionary
    MainData.loadDictionary("en","sowpods.txt")
    MainData.loadSegment("en_easy","en_easy.txt")
    MainData.loadSegment("en_normal","en_normal.txt")
    MainData.loadSegment("en_hard","en_hard.txt")

    runBlocking {

        GlobalScope.launch{
            println("Started timing thread")
            //this is time coroutine
            while(true){
                delay(100)
                for((_,room) in MainData.rooms){
                    if(room.state==GameState.RUNNING){
                        room.tickTime()
                    }else{
                        room.tickKick()
                    }
                }
            }
        }

        val arg:Array<String>
        if(args.size<2) {
            arg = arrayOf("0.0.0.0", "8058")
            println("Warning: Starting on default port. Consider adding two more arguments ip and port")
        }else{
            arg=args
        }

        embeddedServer(Netty, port = arg[1].toInt(), host = arg[0], watchPaths = listOf("build"), module = Application::module)
            .start(wait = true)
    }
}

fun Application.module() {
    configureSerialization()
    configureSockets()
//    configureRouting()
}

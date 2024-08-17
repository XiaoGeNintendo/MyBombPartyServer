package top.hhs.xgn.mybombparty.hack

import io.ktor.websocket.*

suspend fun WebSocketSession.receiveString():String{
    while(true) {
        val frame = incoming.receive()
        if (frame is Frame.Text) {
            return frame.readText()
        }
    }
}

fun String.checkValid(lengthThreshold:Int=30):Boolean{
    if(isEmpty() || length>lengthThreshold){
        return false
    }

    if("#".any { it in this }){
        return false
    }

    return true
}
package top.hhs.xgn.mybombparty.data

import io.ktor.websocket.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Player(val name:String, var life:Int){

    val alive: Boolean
        get() = life!=0

    /**
     * This variable is lazily set.
     *
     * Will only get updated when broadcast or a notification from the websocket server
     */
    var online:Boolean=true

    @Transient
    var session:WebSocketSession?=null

    suspend fun broadcast(message: String) {
        online=isSessionAlive()
        if(online) {
            session?.send(message)
        }
    }

    fun isSessionAlive() = !(session==null || session?.outgoing?.isClosedForSend == true || session?.incoming?.isClosedForReceive == true)

}

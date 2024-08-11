package top.hhs.xgn.mybombparty.data

import kotlinx.serialization.Serializable

@Serializable
data class GameRoomPreview(val name:String,
                           val lang:String,
                           val segments:String,
                           val timeout:Int,
                           val rewardThreshold:Int,
                           val initialLife:Int,
                           val changeAfterFails:Int,
    val playerCount: Int,
    val state: GameState) {

    /**
     * Create a game room ignoring [playerCount] and [state]
     */
    fun toGameRoom()=GameRoom(name,lang,segments,timeout,rewardThreshold,initialLife,changeAfterFails)
}

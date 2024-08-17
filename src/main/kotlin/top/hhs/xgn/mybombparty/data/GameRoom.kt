package top.hhs.xgn.mybombparty.data

import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import top.hhs.xgn.mybombparty.server.MainData

@Serializable
data class GameRoom(val name:String, val lang:String, val segments:String, val timeout:Int, val rewardThreshold:Int, val initialLife:Int, val changeAfterFails:Int) {

    val players=ArrayList<Player>()

    val spectators=ArrayList<Player>()

    val usedWords=HashSet<String>()
    var currentPlayer=0

    var state:GameState=GameState.BEFORE_START

    var currentSegment="?"
    var currentFail=0

    var winner=""
    var timeLeft=0
    fun toPreview() = GameRoomPreview(name,lang,segments,timeout,rewardThreshold,initialLife,changeAfterFails,players.size,state)

    /**
     * Start a new game with current players
     */
    suspend fun start(){
        timeLeft=timeout
        currentPlayer=-1
        usedWords.clear()
        state=GameState.RUNNING
        currentSegment=getRandomSegment()
        currentFail=0

        for(pl in players){
            pl.life=initialLife
        }

        broadcast("startRoom")
        nextPlayer(false)

    }

    fun getRandomSegment():String{
        return MainData.segments[segments]!!.random()
    }

    suspend fun tickKick(){
        if(state==GameState.RUNNING){
            return
        }

        val toRemove=HashSet<Player>()
        players.forEach{
            if(!it.online){
                broadcast("kick ${it.name}")
                toRemove.add(it)
            }
        }

        players.removeAll(toRemove)
    }

    suspend fun broadcast(message:String){
        for(player in players){
            if(player.online) {
                player.broadcast(message)
                if(!player.online){
                    broadcast("disconnect ${player.name}")
                }
            }
        }
        for(spectator in spectators){
            if(spectator.online) {
                spectator.broadcast(message)
            }
        }
    }

    /**
     * Tick down 1 tick and switch to next player if necessary
     */
    suspend fun tickTime() {
        timeLeft--
        if(timeLeft<=0){
            players[currentPlayer].life--
            broadcast("loseLife ${players[currentPlayer].name}")

            nextPlayer(true)
        }else if(timeLeft%10==0){
            broadcast("countdown $timeLeft")
        }
    }

    suspend fun nextPlayer(fail:Boolean){
        timeLeft=timeout

        do {
            currentPlayer = (currentPlayer + 1) % players.size
        }while(!players[currentPlayer].alive)

        if(fail){
            currentFail++
            if(currentFail==changeAfterFails){
                currentSegment=getRandomSegment()
                currentFail=0
            }
        }else{
            currentFail=0
            currentSegment=getRandomSegment()
        }

        if(players.count { it.alive }==1){
            winner=players.first { it.alive }.name
            broadcast("win $winner")
            state=GameState.ENDED

            //kicks all dead player
            var x=0
            while(x<players.size){
                if(!players[x].online){
                    broadcast("kick ${players[x].name}")
                    players.removeAt(x)
                }else{
                    x++
                }
            }
        }else {
            broadcast("new $currentSegment")
            broadcast("start ${players[currentPlayer].name}")
        }
    }

    fun connect(userName: String, session: WebSocketSession):Boolean {
        val thatPlayer=players.firstOrNull{it.name==userName}
        if(thatPlayer==null){
            players.add(Player(userName,initialLife).also{ it.session=session })
            return true
        }else{
            if(!thatPlayer.isSessionAlive()){
                thatPlayer.session=session
                thatPlayer.online=true
                return true
            }
            return false
        }
    }

    fun isValidWord(word:String)= word in MainData.dicts[lang]!!
    fun isUsedWord(word:String) = word in usedWords

    suspend fun processIncomingMessage(userName: String, msg: String) {
        if(state!=GameState.RUNNING){
            return
        }

        if(userName!=players[currentPlayer].name){
            return
        }

        val msgs=msg.split("#")
        if(msgs.size!=2){
            return
        }

        val code=msgs[0]
        val word=msgs[1]
        if(code=="type"){
            broadcast("type $word")
        }else if(code=="confirm"){
            if(!isValidWord(word) || currentSegment !in word){
                broadcast("fail $word")
            }else if(isUsedWord(word)) {
                broadcast("used $word")
            }else{
                broadcast("success $word")

                if(word.length>=rewardThreshold){
                    broadcast("heal $userName")
                    players[currentPlayer].life++
                }

                usedWords.add(word)
                nextPlayer(false)
            }
        }
    }

    fun isAdmin(userName: String): Boolean {
        return players.size>=1 && userName==players[0].name
    }

    /**
     * Kicks a player from the player list
     */
    suspend fun kick(username: String) {
        if(state==GameState.RUNNING){
            throw IllegalStateException("Cannot kick while running")
        }

        val player=players.firstOrNull{it.name==username}
        if(player!=null){
            if(player.isSessionAlive()) {
                player.session?.close(CloseReason(CloseReason.Codes.GOING_AWAY,"Kicked"))
            }
            players.remove(player)
            broadcast("kick $username")
        }
    }

    /**
     * Prepare the room for closing. Closes all players and spectators session
     */
    suspend fun close() {
        broadcast("close")
        for(pl in players){
            if(pl.isSessionAlive()){
                pl.session?.close(CloseReason(CloseReason.Codes.NORMAL,"Room closed"))
            }
        }
        for(pl in spectators){
            if(pl.isSessionAlive()){
                pl.session?.close(CloseReason(CloseReason.Codes.NORMAL,"Room closed"))
            }
        }

        players.clear()
        spectators.clear()
    }

}
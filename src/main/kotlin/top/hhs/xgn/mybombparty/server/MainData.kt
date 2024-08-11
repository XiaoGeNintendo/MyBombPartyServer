package top.hhs.xgn.mybombparty.server

import top.hhs.xgn.mybombparty.data.GameRoom
import java.io.File
import java.nio.charset.Charset

object MainData {

    /**
     * Load dictionary dict/from to name
     */
    fun loadDictionary(name: String, from: String) {
        dicts[name]=File("dict/$from").readLines(Charset.forName("UTF-8")).map { it.trim() }.toHashSet()
        println("Loaded dictionary $name of size ${dicts[name]!!.size}")
    }

    fun loadSegment(name: String, from: String){
        segments[name]=File("seg/$from").readLines(Charset.forName("UTF-8")).map { it.trim() }.toHashSet()
        println("Loaded segment $name of size ${segments[name]!!.size}")
    }

    val rooms=HashMap<String, GameRoom>()
    val dicts=HashMap<String,HashSet<String>>()
    val segments=HashMap<String,HashSet<String>>()
    val requireClientVersion="2"
}
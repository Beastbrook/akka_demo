/**
  * Created by Javis on 2016/12/14 0014.
  */

//Master保存接受到的Worker信息
class WorkerInfo(val id:String,val memory:Int,val cores:Int) {

  //上次心跳更新的时间
   var lastHeartBeatTime: Long = 0

  //更新上次心跳的时间
  def updateLastHeartBeatTime() = {
    lastHeartBeatTime = System.currentTimeMillis
  }
}

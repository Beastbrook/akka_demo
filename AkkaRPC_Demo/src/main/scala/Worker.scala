import java.util.UUID

import akka.actor.{Actor, ActorSelection, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

/** Created by Javis on 2016/12/14 0014 */

class Worker(val masterHost:String,val masterPort:Int) extends Actor{

  //保存使用akka-url获得的master,该对象可以发生消息
  var master:ActorSelection=_

  //worker唯一标识
  val id_Worker=UUID.randomUUID().toString

  //心跳间隔
  val HEARTBEAT_INTERVAL=5000

  override def preStart(): Unit = {
    println("worker-preStart")
    //通过masterUrl获得ActorSelectiond对象，该对象可以发生消息
    master= context.actorSelection(s"akka.tcp://MasterSystem@$masterHost:$masterPort/user/Master")
    val memory=8 //the default of memory is 8G
    val cores=4 //the default number of cores  is 4
    //向master发送注册的信息
    master ! RegisterWorker(id_Worker,memory,cores)
  }

  override def receive: Receive = {
    case RegisteredWorker(masterUrl)=> {//处理注册成功的逻辑
      import context.dispatcher
      //由于第三个参数是传递一个ActorRef对象，但是目前的master是ActorSelection类型，因此先给自己发送下消息
      context.system.scheduler.schedule(0 millis,HEARTBEAT_INTERVAL millis,self,SendHeartBeat)
    }
    case SendHeartBeat=>{ //发生心跳。在此可用添加额外的处理逻辑
      println("定时发生心跳给Master")
      master ! HeartBeat(id_Worker)
    }
  }
}

object Worker{
  def main(args: Array[String]) {
    val host=args(0)
    val port=args(1).toInt
    val masterHost=args(2)
    val masterPort=args(3).toInt
    val confStr=
      s"""
         |akka.actor.provider = "akka.remote.RemoteActorRefProvider"
         |akka.remote.netty.tcp.hostname = "$host"
         |akka.remote.netty.tcp.port = "$port"
       """.stripMargin
    val conf=ConfigFactory.parseString(confStr)
    val actorSystem= ActorSystem("WorkerSystem",conf)
    val master=actorSystem.actorOf(Props(new Worker(masterHost,masterPort)),"Worker")
    actorSystem.awaitTermination
  }
}
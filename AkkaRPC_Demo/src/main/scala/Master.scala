import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

/*** Created by Javis on 2016/12/14 0014. * */

class Master(val host:String,val port:Int) extends Actor{

  //workerId->workerInfo
  val id2WorkInfo=new scala.collection.mutable.HashMap[String,WorkerInfo]

  //为了便于一些额外的逻辑，比如按Worker的剩余可用memory进行排序
  val workers= new scala.collection.mutable.HashSet[WorkerInfo]

  //检查worker是否超时的时间间隔
  val CHECK_INTERVAL=10000



  override def preStart(): Unit = {
    //使用schedule必须导入该扩展的隐式变量
    import context.dispatcher
    //millis是毫秒的单位，其在包scala.concurrent.duration下
    context.system.scheduler.schedule(0 millis,CHECK_INTERVAL millis,self,CheckTimeOutWorker)
  }

  override def receive: Receive = {
    case RegisterWorker(id,memory,cores)=> {//把Worker的注册消息封装到类WorkerInfo中
      if (!id2WorkInfo.contains(id)) {
        //当前workerId没有注册
        val workerInfo = new WorkerInfo(id, memory, cores)
        id2WorkInfo.put(id, workerInfo)
        workers += workerInfo
        //这里简单发生Master的url通知worker注册成功
        sender ! RegisteredWorker(s"akka.tcp://MasterSystem@$host:$port/user/Master")
      }
    }
    case HeartBeat(workerId)=>{ //处理Worker的心跳
      if(id2WorkInfo.contains(workerId)){
        id2WorkInfo(workerId).updateLastHeartBeatTime()
      }
    }
    case CheckTimeOutWorker=>{ //定时检测是否有超时的worker并进行处理
      val cur=System.currentTimeMillis
      //过滤出超时的worker
      val deadWorker= workers.filter(x=>cur-x.lastHeartBeatTime>CHECK_INTERVAL)
      //从记录删除删除超时的worker
      for(w <- deadWorker) {
        id2WorkInfo -= w.id
        workers -= w
      }
      println(workers.size)
    }
  }
}

object Master{
  def main(args: Array[String]) {
    val host=args(0)
    val port=args(1).toInt
    //构造配置参数值，使用3个双引号可以多行，使用s可以在字符串中使用类似Shell的变量
    val confStr=
      s"""
          |akka.actor.provider = "akka.remote.RemoteActorRefProvider"
          |akka.remote.netty.tcp.hostname = "$host"
          |akka.remote.netty.tcp.port = "$port"
       """.stripMargin
    //通过工厂方法获得一个config对象
    val conf=ConfigFactory.parseString(confStr)
    //初始化一个ActorSystem，其名为MasterSystem
    val actorSystem= ActorSystem("MasterSystem",conf)
    //使用actorSystem实例化一个名为Master的actor,注意这个名称在Worker连接Master时会用到
    val master=actorSystem.actorOf(Props(new Master(host,port)),"Master")
    //阻塞当前线程直到系统关闭退出
    actorSystem.awaitTermination
  }
}
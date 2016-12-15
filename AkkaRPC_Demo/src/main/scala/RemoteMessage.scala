/**
  * Created by Javis on 2016/12/14 0014.
  */

//远程通信可序列化特质,需要远程通信的样本类需继承该特质
trait RemoteMessage extends Serializable

//worker向master发生注册消息的
case class RegisterWorker(val id:String,val memory:Int,val cores:Int) extends  RemoteMessage

//master向worker反馈注册成功的信息，这里只简单返回master的url
case class RegisteredWorker(val masterUrl:String) extends  RemoteMessage

//该伴生对象用于worker本地发生消息给自己
case object SendHeartBeat

//worker发生心跳给master
case class HeartBeat(val workerId:String) extends RemoteMessage

//该伴生对象用于master定时检测超时的worker
case object CheckTimeOutWorker
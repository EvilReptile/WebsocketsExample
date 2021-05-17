package Practic.Print

import Chat.ChatMessages.{UserDo, UserJoined, UserLeft, UserSaid}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}

import scala.collection.mutable
import scala.util.Random

class PrintService()(implicit system: ActorSystem, mat: Materializer) {

    private val roomActor = system.actorOf(Props(classOf[PrintConnectionActor]))

    def flow: Flow[Message, Message, Any] = {
        val name = Random.between(10000, 99999)
        val (actor, publisher) =
            Source.actorRef[String](16, OverflowStrategy.dropHead)
                .map(msg => TextMessage.Strict(msg))
                .toMat(Sink.asPublisher(false))(Keep.both).run()

        roomActor ! UserJoined(name.toString, actor)

        val sink: Sink[Message, Any] = Flow[Message]
            .map {
                case TextMessage.Strict(msg) =>
                    roomActor ! UserSaid(name.toString, msg)
            }
            .to(Sink.onComplete(_ =>
                roomActor ! UserLeft(name.toString)
            ))

        Flow.fromSinkAndSource(sink, Source.fromPublisher(publisher))
    }

    def makeEvent(user: String): Unit = {
        roomActor ! UserDo(user)
    }
    
}

class PrintConnectionActor extends Actor {
    var connection: mutable.Map[String, ActorRef] = mutable.Map.empty

    override def receive: Receive = {
        case UserJoined(name, actorRef) =>
            connection.put(name, actorRef)
            println(s"Joined the print server. $name")

        case UserLeft(name) =>
            connection.remove(name)
            println(s"Left the print server.")

        case UserSaid(name, fileName) =>
            println(s"$fileName")
            broadcast(name, fileName)

        case UserDo(user) =>
            println("UserEvent")
            broadcast(user, "test")
    }

    def broadcast(name: String, msg: String): Unit = {
        println("event")
        Thread.sleep(1000)
        connection.get(name).foreach(_ ! s"$msg 25%")
        Thread.sleep(1000)
        connection.get(name).foreach(_ ! s"$msg 50%")
        Thread.sleep(1000)
        connection.get(name).foreach(_ ! s"$msg 75%")
        Thread.sleep(1000)
        connection.get(name).foreach(_ ! s"$msg generated")
    }
}

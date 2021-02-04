package Practic.Core

import Chat.ChatMessages.{UserJoined, UserLeft, UserSaid}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}

import scala.collection.mutable

class CoreService()(implicit system: ActorSystem, mat: Materializer) {

    private val roomActor = system.actorOf(Props(classOf[ConnectionActor]))

    def flow(name: String): Flow[Message, Message, Any] = {
        val (actor, publisher) =
            Source.actorRef[String](16, OverflowStrategy.dropHead)
                .map(msg => TextMessage.Strict(msg))
                .toMat(Sink.asPublisher(false))(Keep.both).run()

        roomActor ! UserJoined(name, actor)

        val sink: Sink[Message, Any] = Flow[Message]
            .map {
                case TextMessage.Strict(msg) =>
                    roomActor ! UserSaid(name, msg)
            }
            .to(Sink.onComplete(_ =>
                roomActor ! UserLeft(name)
            ))

        Flow.fromSinkAndSource(sink, Source.fromPublisher(publisher))
    }

}

class ConnectionActor extends Actor {
    var connection: mutable.Map[String, ActorRef] = mutable.Map.empty
    var printClient: mutable.Map[String, PrintClient] = mutable.Map.empty

    override def receive: Receive = {
        case UserJoined(name, actorRef) =>
            connection.put(name, actorRef)
            printClient.put(name, new PrintClient(name, this))
            println(s"Joined the core server.")
            broadcast(name, s"Hello $name")

        case UserLeft(name) =>
            connection.remove(name)
            printClient.get(name).map(_.left)
            printClient.remove(name)
            println(s"Left the core server.")

        case UserSaid(name, fileName) =>
            println(s"CoreService $fileName")
            sentPrintService(name, fileName)
    }

    def broadcast(name: String, msg: String): Unit = {
        connection.get(name).foreach(_ ! msg)
    }

    def sentPrintService(name: String, msg: String) = {
        printClient.get(name).map(_.addFile(msg))
    }
}

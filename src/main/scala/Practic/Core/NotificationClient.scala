package Practic.Core

import Practic.Core.CoreStates._
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

class PrintClient(name: String, connection: ConnectionActor) {
    implicit val system = ActorSystem()
    private val roomActor = system.actorOf(Props(classOf[PrintClientConnectionActor]))
    val (upgradeResponse, closed) =
        Http().singleWebSocketRequest(WebSocketRequest("ws://localhost:8090/generatePf"), flow)

    private def flow: Flow[Message, Message, Any] = {
        val (actor, publisher) =
            Source.actorRef[String](16, OverflowStrategy.dropHead)
                .map(msg => TextMessage.Strict(msg))
                .toMat(Sink.asPublisher(false))(Keep.both).run()

        roomActor ! Joined(actor)

        val sink: Sink[Message, Any] = Flow[Message]
            .map {
                case TextMessage.Strict(msg) =>
                    println(msg)
                    connection.broadcast(name, msg)
            }
            .to(Sink.onComplete(_ =>
                roomActor ! Left
            ))

        Flow.fromSinkAndSource(sink, Source.fromPublisher(publisher))
    }

    def addFile(filename: String) = {
        roomActor ! Said(filename)
    }

    def left = {
        roomActor ! Left
    }
}

class PrintClientConnectionActor extends Actor {
    var connection: Option[ActorRef] = None

    override def receive: Receive = {
        case Joined(actorRef) =>
            connection = Option(actorRef)
            println(s"Joined the print client.")

        case Left() =>
            connection = None
            println(s"Left the print client.")

        case Said(fileName) =>
            println(s"PrintClient $fileName")
            broadcast(fileName)
    }

    def broadcast(msg: String): Unit = {
        connection.foreach(_ ! msg)
    }
}

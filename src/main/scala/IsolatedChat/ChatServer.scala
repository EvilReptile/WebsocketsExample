package IsolatedChat

import java.util.concurrent.Executors

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.Flow

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object ChatServer extends App {
    implicit val system = ActorSystem("app")
    implicit val materializer = Materializer.matFromSystem
    implicit val executionContext: ExecutionContextExecutor =
        ExecutionContext.fromExecutor(Executors.newFixedThreadPool(16))

    val interface: Route =
        pathEndOrSingleSlash {
            getFromResource("web/index.html")
        } ~
            getFromResourceDirectory("web")

    val chatApi = path("api" / "chat")(get {
        parameters("name") { name =>
            handleWebSocketMessages((new ChatRoom).flow(name))
        }
    })

    Http().bindAndHandle(chatApi ~ interface, "localhost", 8100)
        .map { _ =>
            println(s"Server is running at http://localhost:8100/")
        }
}

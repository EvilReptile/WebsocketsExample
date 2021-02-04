package Chat

import java.util.concurrent.Executors
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object ChatServer extends App {
    implicit val system: ActorSystem = ActorSystem("app")
    implicit val materializer: Materializer = Materializer.matFromSystem
    implicit val executionContext: ExecutionContextExecutor =
        ExecutionContext.fromExecutor(Executors.newFixedThreadPool(16))

    private val chatroom = new ChatRoom()

    val interface: Route =
        pathEndOrSingleSlash {
            getFromResource("web/index.html")
        } ~
            getFromResourceDirectory("web")

    val chatApi: Route =
        path("api" / "chat") {
            get {
                parameters("name") { name =>
                    handleWebSocketMessages(chatroom.flow(name))
                }
            }
        }

    Http().bindAndHandle(chatApi ~ interface, "localhost", 8080)
        .map { _ =>
            println(s"Server is running at http://localhost:8080/")
        }
}

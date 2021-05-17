package Practic.Print

import Chat.ChatMessages.UserDo

import java.util.concurrent.Executors
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object NotificationServer extends App {
    implicit val system = ActorSystem("app")
    implicit val materializer = Materializer.matFromSystem
    implicit val executionContext: ExecutionContextExecutor =
        ExecutionContext.fromExecutor(Executors.newFixedThreadPool(16))

    val printService = new PrintService
    val chatApi = path("generatePf")(get {
        handleWebSocketMessages(printService.flow)
    })

    val eventApi = path("event")((get & parameter('user.as[String])){ user =>
        printService.makeEvent(user)
        complete(OK)
    })
    
    Http().bindAndHandle(chatApi ~ eventApi, "localhost", 8090)
        .map { _ =>
            println(s"Server is running at http://localhost:8090/")
        }

}

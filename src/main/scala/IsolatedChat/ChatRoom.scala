package IsolatedChat

import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.Flow

class ChatRoom {
    def flow(name: String): Flow[Message, Message, Any] = {
        Flow[Message].collect {
            case TextMessage.Strict(t) ⇒ t
        }.map { text ⇒
            TextMessage.Strict(s"$name: $text")
        }
    }
}

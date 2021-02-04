package Practic.Core

import akka.actor.ActorRef

object CoreStates {

  sealed trait CoreEvent

  case class Joined(userActor: ActorRef) extends CoreEvent {
    override def equals(that: Any): Boolean = this.asInstanceOf[Joined].equals(that)
  }

  case class Left() extends CoreEvent {
    override def equals(that: Any): Boolean = this.asInstanceOf[Left].equals(that)
  }

  case class Said(message: String) extends CoreEvent {
    override def equals(that: Any): Boolean = this.asInstanceOf[Said].equals(that)
  }
}

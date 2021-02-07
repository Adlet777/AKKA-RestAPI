package com.example

//#user-registry-actor
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import scala.collection.immutable

//#user-case-classes
final case class Order(productName: String, price: Int, carModel: String)
final case class Orders(orders: immutable.Seq[Order])
//#user-case-classes

object OrderRegistry {
  // actor protocol
  sealed trait Command
  final case class GetOrders(replyTo: ActorRef[Orders]) extends Command
  final case class CreateOrder(order: Order, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class GetOrder(productName: String, replyTo: ActorRef[GetOrderResponse]) extends Command
  final case class DeleteOrder(productName: String, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class GetOrderResponse(maybeOrder: Option[Order])
  final case class ActionPerformed(description: String)

  def apply(): Behavior[Command] = registry(Set.empty)

  private def registry(orders: Set[Order]): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetOrders(replyTo) =>
        replyTo ! Orders(orders.toSeq)
        Behaviors.same
      case CreateOrder(order, replyTo) =>
        replyTo ! ActionPerformed(s"Order ${order.productName} created.")
        registry(orders + order)
      case GetOrder(productName, replyTo) =>
        replyTo ! GetOrderResponse(orders.find(_.productName == productName))
        Behaviors.same
      case DeleteOrder(productName, replyTo) =>
        replyTo ! ActionPerformed(s"Order $productName deleted.")
        registry(orders.filterNot(_.productName == productName))
    }
}
//#user-registry-actor
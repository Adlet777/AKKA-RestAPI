package com.example

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import scala.concurrent.Future
import com.example.OrderRegistry._
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

//#import-json-formats
//#user-routes-class
class Routes(orderRegistry: ActorRef[OrderRegistry.Command])(implicit val system: ActorSystem[_]) {

  //#user-routes-class
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._
  //#import-json-formats

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  def getOrders(): Future[Orders] =
    orderRegistry.ask(GetOrders)
  def getOrder(productName: String): Future[GetOrderResponse] =
    orderRegistry.ask(GetOrder(productName, _))
  def createOrder(order: Order): Future[ActionPerformed] =
    orderRegistry.ask(CreateOrder(order, _))
  def deleteOrder(productName: String): Future[ActionPerformed] =
    orderRegistry.ask(DeleteOrder(productName, _))

  //#all-routes
  //#orders-get-post
  //#orders-get-delete
  val userRoutes: Route =
  pathPrefix("orders") {
    concat(
      //#orders-get-delete
      pathEnd {
        concat(
          get {
            complete(getOrders())
          },
          post {
            entity(as[Order]) { order =>
              onSuccess(createOrder(order)) { performed =>
                complete((StatusCodes.Created, performed))
              }
            }
          })
      },
      //#orders-get-delete
      //#orders-get-post
      path(Segment) { productName =>
        concat(
          get {
            //#retrieve-order-info
            rejectEmptyResponse {
              onSuccess(getOrder(productName)) { response =>
                complete(response.maybeOrder)
              }
            }
            //#retrieve-order-info
          },
          delete {
            //#orders-delete-logic
            onSuccess(deleteOrder(productName)) { performed =>
              complete((StatusCodes.OK, performed))
            }
            //#orders-delete-logic
          })
      })
    //#orders-get-delete
  }
  //#all-routes
}

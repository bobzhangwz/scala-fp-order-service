package com.zhpooer.ecommerce.order.order

import cats.effect.{Blocker, ContextShift, ExitCase, Sync}
import cats.implicits._
import fs2.Stream
import io.circe.{Decoder, parser}
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.{DeleteMessageRequest, Message, ReceiveMessageRequest}

import scala.jdk.CollectionConverters._

class SQSEventSubscriber[F[_]: Sync: ContextShift, E: Decoder](sqsClient: SqsClient, eventQueueUrl: String, blocker: Blocker) {
  def fetchMessage: F[List[Message]] = {
    val receiveMessageRequest = ReceiveMessageRequest.builder()
      .queueUrl(eventQueueUrl)
      .maxNumberOfMessages(5)
      .build()
    blocker.delay {
      sqsClient.receiveMessage(receiveMessageRequest).messages().asScala.toList
    }
  }

  def deleteMessage(msg: Message): F[Unit]= {
    val deleteMessageRequest = DeleteMessageRequest.builder()
      .queueUrl(eventQueueUrl)
      .receiptHandle(msg.receiptHandle())
      .build()
    blocker.delay {
      sqsClient.deleteMessage(deleteMessageRequest)
    }.void
  }

  def eventStream: Stream[F, E] = {
    for {
      msgList <- Stream.repeatEval(fetchMessage)
      msg <- msgList.map(m =>
        Stream.bracketCase(m.pure[F]){
          case (mm, ExitCase.Completed) => deleteMessage(mm)
          case _ => Sync[F].pure(())
        }).reduce(_ ++ _)
      event <- Stream.eval {
        val errorOrEvent = for {
          json <- parser.parse(msg.body())
          e <- json.as[E]
        } yield e
        Sync[F].fromEither(errorOrEvent)
      }
    } yield event
  }
}

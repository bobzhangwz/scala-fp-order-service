package com.zhpooer.ecommerce.infrastructure.event

import cats.effect.{Blocker, ContextShift, ExitCase, Sync}
import cats.implicits._
import fs2.Stream
import io.circe.{Decoder, parser}
import org.log4s.getLogger
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.{DeleteMessageRequest, Message, ReceiveMessageRequest}

import scala.jdk.CollectionConverters._
import scala.concurrent.duration._

class SQSEventSubscriber[F[_]: Sync: ContextShift, E: Decoder](sqsClient: SqsClient, eventQueueUrl: String, blocker: Blocker) {
  private[this] val logger = getLogger

  def fetchMessage: F[List[Message]] = {
    val receiveMessageRequest = ReceiveMessageRequest.builder()
      .queueUrl(eventQueueUrl)
      .maxNumberOfMessages(5)
      .waitTimeSeconds(20.seconds.toSeconds.toInt)
      .build()
    Sync[F].delay { logger.info(s"Start pulling sqs messages. ${receiveMessageRequest.toString}") } >> blocker.delay {
      sqsClient.receiveMessage(receiveMessageRequest).messages().asScala.toList
    }
  }

  def deleteMessage(msg: Message): F[Unit]= {
    val deleteMessageRequest = DeleteMessageRequest.builder()
      .queueUrl(eventQueueUrl)
      .receiptHandle(msg.receiptHandle())
      .build()
    Sync[F].delay { logger.info(s"Deleting msg: ${msg.messageId()}") } >> blocker.delay {
      sqsClient.deleteMessage(deleteMessageRequest)
    }.void
  }

  def eventStream: Stream[F, E] = {
    for {
      msgList <- Stream.repeatEval(fetchMessage)
      msg <- Stream(msgList: _*).flatMap(m =>
        Stream.bracketCase(m.pure[F]){
          case (mm, ExitCase.Completed) =>
            deleteMessage(mm)
          case (mm, ExitCase.Error(e)) =>
            Sync[F].delay { logger.error(e)(s"Error to handle message ${mm.messageId()}") }
          case (mm, ExitCase.Canceled) =>
            Sync[F].delay { logger.info(s"Canceled, ${mm.messageId()}") }
        })
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


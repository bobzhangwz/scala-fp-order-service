#!/bin/bash

awslocal sqs create-queue --queue-name order/order-event-listener
awslocal sns create-topic --name order-event-publisher
awslocal sns subscribe \
         --topic-arn  arn:aws:sns:ap-east-1:000000000000:order-event-publisher\
         --protocol sns \
         --notification-endpoint http://localhost:4566/000000000000/order/order-event-listener

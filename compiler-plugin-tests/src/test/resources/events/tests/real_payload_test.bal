import ballerina/test;
import ballerinax/aws.lambda;

// Captured from a real SNS topic in eu-north-1, 2026-08-21. Signature truncated.
// Note Subject is an explicit null and MessageAttributes an empty object, not absent.
json realSnsNoSubject = {
    "Records": [{
        "EventSource": "aws:sns",
        "EventVersion": "1.0",
        "EventSubscriptionArn": "arn:aws:sns:eu-north-1:123456789012:example-topic:10c406f0",
        "Sns": {
            "Type": "Notification",
            "MessageId": "8ef28fd8-4365-560b-a3d3-ecda4423b322",
            "TopicArn": "arn:aws:sns:eu-north-1:123456789012:example-topic",
            "Message": "bare message",
            "Timestamp": "2026-08-21T07:57:58.303Z",
            "SignatureVersion": "1",
            "Signature": "kDeYkjMznqX8fJ0zQMqKn",
            "SigningCertUrl": "https://sns.eu-north-1.amazonaws.com/SimpleNotificationService-7506a1e.pem",
            "Subject": null,
            "UnsubscribeUrl": "https://sns.eu-north-1.amazonaws.com/?Action=Unsubscribe",
            "MessageAttributes": {}
        }
    }]
};

// Captured from a real EventBridge rule in eu-north-1, 2026-08-21.
json realEventBridge = {
    "version": "0",
    "id": "ccb45c32-7e54-a5a2-9f5f-33dcf67e89b8",
    "detail-type": "Ballerina Shape Test",
    "source": "example.source",
    "account": "123456789012",
    "time": "2026-08-21T07:58:09Z",
    "region": "eu-north-1",
    "resources": [],
    "detail": {"hello": "world", "n": 42}
};

@test:Config {}
function testRealSnsWithNullSubject() returns error? {
    lambda:SNSEvent event = check realSnsNoSubject.cloneWithType();
    test:assertEquals(event.Records[0].Sns.Message, "bare message");
    test:assertTrue(event.Records[0].Sns.Subject is ());
}

@test:Config {}
function testRealEventBridge() returns error? {
    lambda:EventBridgeEvent event = check realEventBridge.cloneWithType();
    test:assertEquals(event.detail\-type, "Ballerina Shape Test");
    test:assertEquals(event.resources.length(), 0);
}

// Captured from a real Kinesis stream in eu-north-1, 2026-08-21 - a three record batch,
// which is how Lambda actually delivers Kinesis records.
json realKinesisBatch = {
    "Records": [
        {
            "kinesis": {
                "kinesisSchemaVersion": "1.0", "partitionKey": "pk-1",
                "sequenceNumber": "49677587210188986285676566057046426382227441142810542082",
                "data": "aGVsbG8gb25l", "approximateArrivalTimestamp": 1787300037.612
            },
            "eventSource": "aws:kinesis", "eventVersion": "1.0",
            "eventID": "shardId-000000000000:49677587210188986285676566057046426382227441142810542082",
            "eventName": "aws:kinesis:record",
            "invokeIdentityArn": "arn:aws:iam::123456789012:role/example-lambda-role",
            "awsRegion": "eu-north-1",
            "eventSourceARN": "arn:aws:kinesis:eu-north-1:123456789012:stream/example-stream"
        },
        {
            "kinesis": {
                "kinesisSchemaVersion": "1.0", "partitionKey": "pk-2",
                "sequenceNumber": "49677587210188986285676566057047635308047055771985248258",
                "data": "aGVsbG8gdHdv", "approximateArrivalTimestamp": 1787300037.614
            },
            "eventSource": "aws:kinesis", "eventVersion": "1.0",
            "eventID": "shardId-000000000000:49677587210188986285676566057047635308047055771985248258",
            "eventName": "aws:kinesis:record",
            "invokeIdentityArn": "arn:aws:iam::123456789012:role/example-lambda-role",
            "awsRegion": "eu-north-1",
            "eventSourceARN": "arn:aws:kinesis:eu-north-1:123456789012:stream/example-stream"
        },
        {
            "kinesis": {
                "kinesisSchemaVersion": "1.0", "partitionKey": "pk-3",
                "sequenceNumber": "49677587210188986285676566057048844233866670401159954434",
                "data": "aGVsbG8gdGhyZWU=", "approximateArrivalTimestamp": 1787300037.614
            },
            "eventSource": "aws:kinesis", "eventVersion": "1.0",
            "eventID": "shardId-000000000000:49677587210188986285676566057048844233866670401159954434",
            "eventName": "aws:kinesis:record",
            "invokeIdentityArn": "arn:aws:iam::123456789012:role/example-lambda-role",
            "awsRegion": "eu-north-1",
            "eventSourceARN": "arn:aws:kinesis:eu-north-1:123456789012:stream/example-stream"
        }
    ]
};

@test:Config {}
function testRealKinesisBatch() returns error? {
    lambda:KinesisEvent event = check realKinesisBatch.cloneWithType();
    test:assertEquals(event.Records.length(), 3);
    test:assertEquals(event.Records[0].kinesis.partitionKey, "pk-1");
    test:assertEquals(event.Records[2].kinesis.partitionKey, "pk-3");
    test:assertEquals(event.Records[0].kinesis.approximateArrivalTimestamp, 1787300037.612d);
    test:assertEquals(event.Records[1].invokeIdentityArn,
            "arn:aws:iam::123456789012:role/example-lambda-role");
    // data is always Base64 encoded on the wire.
    test:assertEquals(event.Records[0].kinesis.data, "aGVsbG8gb25l");
}

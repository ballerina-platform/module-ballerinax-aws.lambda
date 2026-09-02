import ballerina/test;
import ballerinax/aws.lambda;

json ebSample = {
    "version": "0",
    "id": "6a7e8feb-b491-4cf7-a9f1-bf3703467718",
    "detail-type": "EC2 Instance State-change Notification",
    "source": "aws.ec2",
    "account": "111122223333",
    "time": "2017-12-22T18:43:48Z",
    "region": "us-west-1",
    "resources": ["arn:aws:ec2:us-west-1:123456789012:instance/i-1234567890abcdef0"],
    "detail": {"instance-id": "i-1234567890abcdef0", "state": "terminated"}
};

json snsSample = {
    "Records": [{
        "EventVersion": "1.0",
        "EventSubscriptionArn": "arn:aws:sns:us-east-2:123456789012:sns-lambda:21be56ed",
        "EventSource": "aws:sns",
        "Sns": {
            "SignatureVersion": "1",
            "Timestamp": "2019-01-02T12:45:07.000Z",
            "Signature": "tcc6faL2yUC6dgZdmrwh1Y4cGa/ebXEkAi6RibDsvpi+tE/1+82j",
            "SigningCertUrl": "https://sns.us-east-2.amazonaws.com/SimpleNotification.pem",
            "MessageId": "95df01b4-ee98-5cb9-9903-4c221d41eb5e",
            "Message": "Hello from SNS!",
            "MessageAttributes": {
                "Test": {"Type": "String", "Value": "TestString"},
                "TestBinary": {"Type": "Binary", "Value": "TestBinary"}
            },
            "Type": "Notification",
            "UnsubscribeUrl": "https://sns.us-east-2.amazonaws.com/?Action=Unsubscribe",
            "TopicArn": "arn:aws:sns:us-east-2:123456789012:sns-lambda",
            "Subject": "TestInvoke"
        }
    }]
};

json kinesisSample = {
    "Records": [{
        "kinesis": {
            "kinesisSchemaVersion": "1.0",
            "partitionKey": "1",
            "sequenceNumber": "49590338271490256608559692538361571095921575989136588898",
            "data": "SGVsbG8sIHRoaXMgaXMgYSB0ZXN0Lg==",
            "approximateArrivalTimestamp": 1545084650.987
        },
        "eventSource": "aws:kinesis",
        "eventVersion": "1.0",
        "eventID": "shardId-000000000006:4959033827149025660855969253836157109592157598913",
        "eventName": "aws:kinesis:record",
        "invokeIdentityArn": "arn:aws:iam::123456789012:role/lambda-role",
        "awsRegion": "us-east-2",
        "eventSourceARN": "arn:aws:kinesis:us-east-2:123456789012:stream/lambda-stream"
    }]
};

@test:Config {}
function testEventBridgeEvent() returns error? {
    lambda:EventBridgeEvent event = check ebSample.cloneWithType();
    test:assertEquals(event.'source, "aws.ec2");
    test:assertEquals(event.detail\-type, "EC2 Instance State-change Notification");
    test:assertEquals(event.resources.length(), 1);
}

@test:Config {}
function testSNSEvent() returns error? {
    lambda:SNSEvent event = check snsSample.cloneWithType();
    test:assertEquals(event.Records[0].Sns.Message, "Hello from SNS!");
    test:assertEquals(event.Records[0].Sns.Subject, "TestInvoke");
    test:assertEquals(event.Records[0].Sns.MessageAttributes["Test"]?.Value, "TestString");
}

@test:Config {}
function testKinesisEvent() returns error? {
    lambda:KinesisEvent event = check kinesisSample.cloneWithType();
    test:assertEquals(event.Records[0].kinesis.partitionKey, "1");
    test:assertEquals(event.Records[0].kinesis.approximateArrivalTimestamp, 1545084650.987d);
    test:assertEquals(event.Records[0].eventSource, "aws:kinesis");
}

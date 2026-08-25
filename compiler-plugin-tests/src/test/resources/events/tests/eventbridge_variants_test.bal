import ballerina/test;
import ballerinax/aws.lambda;

// Captured from a real rate(1 minute) schedule in eu-north-1, 2026-08-21.
// Note resources is NON-empty here - it carries the rule ARN - and detail is empty.
json realScheduledEvent = {
    "version": "0",
    "id": "56b4e114-f17b-da8f-7859-22f0acbde2d9",
    "detail-type": "Scheduled Event",
    "source": "aws.events",
    "account": "123456789012",
    "time": "2026-08-21T08:25:52Z",
    "region": "eu-north-1",
    "resources": ["arn:aws:events:eu-north-1:123456789012:rule/example-schedule-rule"],
    "detail": {}
};

// Captured from a real S3 Object Created event delivered via EventBridge, 2026-08-21.
// A genuine AWS service event, with a richly nested detail object.
json realS3Event = {
    "version": "0",
    "id": "33e742ef-0ab6-47f0-7cea-d9c7fb8423a1",
    "detail-type": "Object Created",
    "source": "aws.s3",
    "account": "123456789012",
    "time": "2026-08-21T08:26:55Z",
    "region": "eu-north-1",
    "resources": ["arn:aws:s3:::example-bucket"],
    "detail": {
        "version": "0",
        "event-version": "1.1",
        "bucket": {"name": "example-bucket"},
        "object": {
            "key": "test/hello.txt", "size": 27,
            "etag": "a06dc9dcd13702a64e6447990af19d7a",
            "sequencer": "006A880BCF49CC2C5F"
        },
        "request-id": "K5WPTH2HGG0K6VK8",
        "requester": "123456789012",
        "source-ip-address": "203.0.113.1",
        "reason": "PutObject"
    }
};

@test:Config {}
function testRealScheduledEvent() returns error? {
    lambda:EventBridgeEvent event = check realScheduledEvent.cloneWithType();
    test:assertEquals(event.'source, "aws.events");
    test:assertEquals(event.detail\-type, "Scheduled Event");
    // The field I was unsure about: populated for schedules, empty for custom events.
    test:assertEquals(event.resources.length(), 1);
    test:assertEquals(event.detail, {});
}

@test:Config {}
function testRealS3ServiceEvent() returns error? {
    lambda:EventBridgeEvent event = check realS3Event.cloneWithType();
    test:assertEquals(event.'source, "aws.s3");
    test:assertEquals(event.detail\-type, "Object Created");
    test:assertEquals(event.resources[0], "arn:aws:s3:::example-bucket");
    // detail is typed as json, so arbitrary service-specific structure survives.
    json detail = event.detail;
    test:assertEquals(check detail.reason, "PutObject");
    json obj = check detail.'object;
    test:assertEquals(check obj.key, "test/hello.txt");
    test:assertEquals(check obj.size, 27);
}

import ballerina/test;
import ballerinax/aws.lambda;

// A real event carrying fields we never declared (AWS adds fields over time).
json ebWithExtras = {
    "version": "0",
    "id": "abc",
    "detail-type": "Scheduled Event",
    "source": "aws.events",
    "account": "111122223333",
    "time": "2026-08-20T00:00:00Z",
    "region": "us-east-1",
    "resources": [],
    "detail": {},
    "replay-name": "some-replay",
    "brandNewFieldAwsAddedLater": {"nested": true}
};

// A real event MISSING a field we declared as required.
json ebMissingResources = {
    "version": "0",
    "id": "abc",
    "detail-type": "Scheduled Event",
    "source": "aws.events",
    "account": "111122223333",
    "time": "2026-08-20T00:00:00Z",
    "region": "us-east-1",
    "detail": {}
};

// Undeclared fields must not break deserialization - AWS adds event fields over time.
@test:Config {}
function testUnknownFieldsTolerated() returns error? {
    lambda:EventBridgeEvent event = check ebWithExtras.cloneWithType();
    test:assertEquals(event.'source, "aws.events");
}

// Documents the failure mode: a field declared required but absent from the event
// fails the whole conversion. Any field AWS does not guarantee must be optional.
@test:Config {}
function testMissingRequiredFieldFailsConversion() {
    lambda:EventBridgeEvent|error event = ebMissingResources.cloneWithType();
    test:assertTrue(event is error, "expected conversion to fail when a required field is absent");
}

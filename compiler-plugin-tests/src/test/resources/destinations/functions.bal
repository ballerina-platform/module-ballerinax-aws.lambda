import ballerinax/aws.lambda;

// No annotation value. Must stay valid and produce no destinations.
@lambda:Function
public function noConfig(lambda:Context ctx, json input) returns json {
    return input;
}

// An empty annotation value. Also produces no destinations.
@lambda:Function {}
public function emptyConfig(lambda:Context ctx, json input) returns json {
    return input;
}

@lambda:Function {
    destinations: {
        onSuccess: "arn:aws:sqs:us-west-1:123456789012:success-queue",
        onFailure: "arn:aws:sns:us-west-1:123456789012:failure-topic"
    }
}
public function bothDestinations(lambda:Context ctx, json input) returns json {
    return input;
}

@lambda:Function {
    destinations: {onFailure: "arn:aws:sns:us-west-1:123456789012:failure-topic"}
}
public function failureOnly(lambda:Context ctx, json input) returns json {
    return input;
}

@lambda:Function {
    destinations: {onSuccess: "arn:aws:sqs:us-west-1:123456789012:success-queue"}
}
public function successOnly(lambda:Context ctx, json input) returns json {
    return input;
}

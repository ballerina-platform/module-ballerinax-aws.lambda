import ballerina/test;
import ballerinax/aws.lambda;

// Auth type NONE - authorizer and authentication are null.
json urlNoAuth = {
    "version": "2.0", "routeKey": "$default", "rawPath": "/my/path",
    "rawQueryString": "parameter1=value1", "cookies": ["cookie1"],
    "headers": {"header1": "value1"}, "queryStringParameters": {"parameter1": "value1"},
    "requestContext": {
        "accountId": "123456789012", "apiId": "33anwqw8fj",
        "authentication": null, "authorizer": null,
        "domainName": "33anwqw8fj.lambda-url.us-west-2.on.aws", "domainPrefix": "33anwqw8fj",
        "http": {"method": "POST", "path": "/my/path", "protocol": "HTTP/1.1",
                 "sourceIp": "123.123.123.123", "userAgent": "agent"},
        "requestId": "e1506fd5", "routeKey": "$default", "stage": "$default",
        "time": "12/Mar/2020:19:03:58 +0000", "timeEpoch": 1583348638390
    },
    "body": "Hello from client!", "pathParameters": null,
    "isBase64Encoded": false, "stageVariables": null
};

// Auth type AWS_IAM - authorizer carries the caller identity.
json urlIamAuth = {
    "version": "2.0", "routeKey": "$default", "rawPath": "/",
    "rawQueryString": "", "headers": {"host": "x.lambda-url.us-west-2.on.aws"},
    "requestContext": {
        "accountId": "123456789012", "apiId": "33anwqw8fj",
        "authentication": null,
        "authorizer": {"iam": {
            "accessKey": "EXAMPLE-ACCESS-KEY-ID", "accountId": "111122223333",
            "callerId": "AIDACKCEVSQ6C2EXAMPLE", "cognitoIdentity": null,
            "principalOrgId": null, "userArn": "arn:aws:iam::111122223333:user/example-user",
            "userId": "AIDACOSFODNN7EXAMPLE2"
        }},
        "domainName": "x.lambda-url.us-west-2.on.aws", "domainPrefix": "x",
        "http": {"method": "GET", "path": "/", "protocol": "HTTP/1.1",
                 "sourceIp": "1.2.3.4", "userAgent": "curl/8.0"},
        "requestId": "abc", "routeKey": "$default", "stage": "$default",
        "time": "07/Sep/2021:22:50:22 +0000", "timeEpoch": 1631055022677
    },
    "isBase64Encoded": false
};

@test:Config {}
function testFunctionURLRequestNoAuth() returns error? {
    lambda:FunctionURLRequest req = check urlNoAuth.cloneWithType();
    test:assertEquals(req.version, "2.0");
    test:assertEquals(req.requestContext.http.method, "POST");
    test:assertEquals(req.rawPath, "/my/path");
    test:assertEquals(req?.body, "Hello from client!");
    test:assertEquals(req.requestContext.timeEpoch, 1583348638390);
}

// A GET with no query, body or cookies must still deserialize - those fields are optional.
@test:Config {}
function testFunctionURLRequestIamAuth() returns error? {
    lambda:FunctionURLRequest req = check urlIamAuth.cloneWithType();
    test:assertEquals(req.requestContext.http.method, "GET");
    test:assertTrue(req?.body is ());
    test:assertTrue(req?.cookies is ());
    lambda:FunctionURLAuthorizer? authorizer = req.requestContext?.authorizer;
    if authorizer is () {
        test:assertFail("expected an authorizer for AWS_IAM auth");
    }
    test:assertEquals(authorizer?.iam?.userId, "AIDACOSFODNN7EXAMPLE2");
}

// Every response field is optional - an empty response is legal and means "AWS, you decide".
@test:Config {}
function testFunctionURLResponseIsAllOptional() {
    lambda:FunctionURLResponse empty = {};
    test:assertEquals(empty.toJson(), {});

    lambda:FunctionURLResponse full = {
        statusCode: 201, headers: {"Content-Type": "application/json"},
        body: "{}", cookies: ["a=b"], isBase64Encoded: false
    };
    test:assertEquals(full.statusCode, 201);
}

// Captured from a real Lambda function URL with AWS_IAM auth, eu-north-1, 2026-08-21.
// headers omitted here - the real ones carry the signing token.
// Note "authentication" is absent entirely, though AWS documents it as null. That is why
// authorizer is declared optional as well as nilable: a NONE auth request may omit it too.
json realFunctionUrlIam = {
    "version": "2.0",
    "routeKey": "$default",
    "rawPath": "/some/path",
    "rawQueryString": "parameter1=value1&parameter2=value",
    "headers": {"x-forwarded-proto": "https", "user-agent": "curl/8.21.0"},
    "queryStringParameters": {"parameter2": "value", "parameter1": "value1"},
    "requestContext": {
        "accountId": "123456789012",
        "apiId": "ajknedaf7zbiqzvhujlmpvt24i0nzota",
        "authorizer": {
            "iam": {
                "accessKey": "EXAMPLE-ACCESS-KEY-ID",
                "accountId": "123456789012",
                "callerId": "AROACKCEVSQ6C2EXAMPLE:example-user",
                "cognitoIdentity": null,
                "principalOrgId": "o-exampleorgid",
                "userArn": "arn:aws:sts::123456789012:assumed-role/example-role/example-user",
                "userId": "AROACKCEVSQ6C2EXAMPLE:example-user"
            }
        },
        "domainName": "ajknedaf7zbiqzvhujlmpvt24i0nzota.lambda-url.eu-north-1.on.aws",
        "domainPrefix": "ajknedaf7zbiqzvhujlmpvt24i0nzota",
        "http": {"method": "GET", "path": "/some/path", "protocol": "HTTP/1.1",
                 "sourceIp": "203.0.113.1", "userAgent": "curl/8.21.0"},
        "requestId": "d78a6551-02e4-4b5e-9ff2-bc84b40c7ab0",
        "routeKey": "$default", "stage": "$default",
        "time": "21/Aug/2026:08:50:19 +0000", "timeEpoch": 1787302219538
    },
    "isBase64Encoded": false
};

// The same request shape but with authorizer absent, as a NONE auth request would be.
json realFunctionUrlNoAuthorizer = {
    "version": "2.0", "routeKey": "$default", "rawPath": "/", "rawQueryString": "",
    "headers": {"x-forwarded-proto": "https"},
    "requestContext": {
        "accountId": "123456789012", "apiId": "abc",
        "domainName": "abc.lambda-url.eu-north-1.on.aws", "domainPrefix": "abc",
        "http": {"method": "GET", "path": "/", "protocol": "HTTP/1.1",
                 "sourceIp": "1.2.3.4", "userAgent": "curl/8.21.0"},
        "requestId": "r1", "routeKey": "$default", "stage": "$default",
        "time": "21/Aug/2026:08:50:19 +0000", "timeEpoch": 1787302219538
    },
    "isBase64Encoded": false
};

@test:Config {}
function testRealFunctionUrlIamAuth() returns error? {
    lambda:FunctionURLRequest req = check realFunctionUrlIam.cloneWithType();
    test:assertEquals(req.requestContext.http.method, "GET");
    test:assertEquals(req.requestContext.timeEpoch, 1787302219538);
    test:assertEquals(req.requestContext?.authorizer?.iam?.principalOrgId, "o-exampleorgid");
    test:assertTrue(req.requestContext?.authorizer?.iam?.cognitoIdentity is ());
    // A GET with no body or cookies must still deserialize.
    test:assertTrue(req?.body is ());
    test:assertTrue(req?.cookies is ());
}

@test:Config {}
function testRealFunctionUrlAuthorizerAbsent() returns error? {
    lambda:FunctionURLRequest req = check realFunctionUrlNoAuthorizer.cloneWithType();
    test:assertEquals(req.rawPath, "/");
    test:assertTrue(req.requestContext?.authorizer is ());
}

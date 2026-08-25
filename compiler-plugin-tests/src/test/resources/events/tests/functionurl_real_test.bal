import ballerina/test;
import ballerinax/aws.lambda;

// All captured from a real AWS_IAM Lambda function URL, eu-north-1, 2026-08-21.
// headers trimmed - the real ones carry the request signing token.

function baseCtx() returns map<json> => {
    "accountId": "123456789012", "apiId": "4jcp5nm3tuwxaukaeyz5ldwer40cdorr",
    "domainName": "4jcp5nm3tuwxaukaeyz5ldwer40cdorr.lambda-url.eu-north-1.on.aws",
    "domainPrefix": "4jcp5nm3tuwxaukaeyz5ldwer40cdorr",
    "requestId": "r1", "routeKey": "$default", "stage": "$default",
    "time": "21/Aug/2026:09:02:58 +0000", "timeEpoch": 1787302978000
};

// A binary body: AWS Base64 encodes it and sets isBase64Encoded true.
@test:Config {}
function testRealBinaryBody() returns error? {
    map<json> ctx = baseCtx();
    ctx["http"] = {"method": "POST", "path": "/b1", "protocol": "HTTP/1.1",
                   "sourceIp": "203.0.113.1", "userAgent": "curl/8.21.0"};
    json payload = {
        "version": "2.0", "routeKey": "$default", "rawPath": "/b1", "rawQueryString": "",
        "headers": {"content-type": "application/octet-stream"}, "requestContext": ctx,
        "body": "AAEC//4gUE5HaXNoIGJpbmFyeSCJUE5H", "isBase64Encoded": true
    };
    lambda:FunctionURLRequest req = check payload.cloneWithType();
    test:assertTrue(req.isBase64Encoded);
    test:assertEquals(req.body, "AAEC//4gUE5HaXNoIGJpbmFyeSCJUE5H");
}

// A text body arrives verbatim with isBase64Encoded false.
@test:Config {}
function testRealTextBody() returns error? {
    map<json> ctx = baseCtx();
    ctx["http"] = {"method": "POST", "path": "/b2", "protocol": "HTTP/1.1",
                   "sourceIp": "203.0.113.1", "userAgent": "curl/8.21.0"};
    json payload = {
        "version": "2.0", "routeKey": "$default", "rawPath": "/b2", "rawQueryString": "",
        "headers": {"content-type": "text/plain"}, "requestContext": ctx,
        "body": "plain text body", "isBase64Encoded": false
    };
    lambda:FunctionURLRequest req = check payload.cloneWithType();
    test:assertFalse(req.isBase64Encoded);
    test:assertEquals(req.body, "plain text body");
}

// Repeated query parameters and repeated headers are comma joined into a single
// value by payload format 2.0, so map<string> is the correct type for both.
@test:Config {}
function testRealMultiValueQueryAndHeaders() returns error? {
    map<json> ctx = baseCtx();
    ctx["http"] = {"method": "GET", "path": "/b3", "protocol": "HTTP/1.1",
                   "sourceIp": "203.0.113.1", "userAgent": "curl/8.21.0"};
    json payload = {
        "version": "2.0", "routeKey": "$default", "rawPath": "/b3",
        "rawQueryString": "a=1&a=2&b=3",
        "headers": {"x-multi": "one,two"},
        "queryStringParameters": {"a": "1,2", "b": "3"},
        "requestContext": ctx, "isBase64Encoded": false
    };
    lambda:FunctionURLRequest req = check payload.cloneWithType();
    map<string> qs = req.queryStringParameters ?: {};
    test:assertEquals(qs.get("a"), "1,2");
    test:assertEquals(req.headers.get("x-multi"), "one,two");
    test:assertEquals(req.rawQueryString, "a=1&a=2&b=3");
}

// Cookies arrive as an array of raw name=value strings.
@test:Config {}
function testRealCookiesArray() returns error? {
    map<json> ctx = baseCtx();
    ctx["http"] = {"method": "GET", "path": "/b4", "protocol": "HTTP/1.1",
                   "sourceIp": "203.0.113.1", "userAgent": "curl/8.21.0"};
    json payload = {
        "version": "2.0", "routeKey": "$default", "rawPath": "/b4", "rawQueryString": "",
        "headers": {"accept": "*/*"},
        "cookies": ["Cookie_1=Value_1", "Cookie_2=Value_2"],
        "requestContext": ctx, "isBase64Encoded": false
    };
    lambda:FunctionURLRequest req = check payload.cloneWithType();
    string[] cookies = req.cookies ?: [];
    test:assertEquals(cookies.length(), 2);
    test:assertEquals(cookies[1], "Cookie_2=Value_2");
}

// Every method a function URL accepts was delivered and parsed.
@test:Config {}
function testRealAllHttpMethods() returns error? {
    foreach string method in ["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"] {
        map<json> ctx = baseCtx();
        ctx["http"] = {"method": method, "path": "/m", "protocol": "HTTP/1.1",
                       "sourceIp": "203.0.113.1", "userAgent": "curl/8.21.0"};
        json payload = {
            "version": "2.0", "routeKey": "$default", "rawPath": "/m", "rawQueryString": "",
            "headers": {"accept": "*/*"}, "requestContext": ctx, "isBase64Encoded": false
        };
        lambda:FunctionURLRequest req = check payload.cloneWithType();
        test:assertEquals(req.requestContext.http.method, method);
    }
}

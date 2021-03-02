# S3Identity.
#
# + principalId - S3 principalId   
public type S3Identity record {
    string principalId;
};

# S3Bucket details.
#
# + name - Parameter Description  
# + arn - Parameter Description  
# + ownerIdentity - Parameter Description  
public type S3Bucket record {
    string name;
    S3Identity ownerIdentity;
    string arn;
};

# S3Object 
#
# + size - Object size
# + eTag - Object tag
# + key - Object key
# + sequencer - Object sequencer  
public type S3Object record {
    string key;
    int size;
    string eTag;
    string sequencer;
};

# S3Element
#
# + bucket - @S3Bucket configuration
# + s3SchemaVersion - s3SchemaVersion
# + configurationId - Configuration id
# + object - @S3Object config
public type S3Element record {
    string s3SchemaVersion;
    string configurationId;
    S3Bucket bucket;
    S3Object 'object;
};

# S3Record 
#
# + s3 - @S3Element 
# + awsRegion - awsRegion 
# + eventVersion - eventVersion
# + responseElements - responseElements
# + eventSource - eventSource
# + eventTime - eventTime
# + requestParameters - requestParameters
# + eventName - eventName
# + userIdentity - @S3Identity
public type S3Record record {
    string eventVersion;
    string eventSource;
    string awsRegion;
    string eventTime;
    string eventName;
    S3Identity userIdentity;
    map<string> requestParameters;
    map<string> responseElements;
    S3Element s3;
};

# S3Event
#
# + Records - @S3Record array
public type S3Event record {
    S3Record[] Records;
};

# SQSRecord
#
# + awsRegion - awsRegion
# + messageAttributes - messageAttributes
# + eventSourceARN - eventSourceARN
# + eventSource - eventSource
# + messageId - messageId
# + receiptHandle - receiptHandle
# + md5OfBody - md5OfBody 
# + attributes - attributes
# + body - body
public type SQSRecord record {
    string messageId;
    string receiptHandle;
    string body;
    map<string> attributes;
    map<string> messageAttributes;
    string md5OfBody;
    string eventSource;
    string eventSourceARN;
    string awsRegion;
};

# SQSEvent
#
# + Records - @SQSRecord array
public type SQSEvent record {
    SQSRecord[] Records;
};

# APIGatewayProxyRequest
#
# + resource - resource
# + path - Path
# + headers - Headers
# + pathParameters - pathParameters
# + isBase64Encoded - isBase64Encoded
# + multiValueQueryStringParameters - multiValueQueryStringParameters
# + requestContext - requestContext
# + multiValueHeaders - multiValueHeaders
# + httpMethod - httpMethod
# + queryStringParameters - queryStringParameters
# + stageVariables - stageVariables
# + body - body
public type APIGatewayProxyRequest record {
    string 'resource;
    string path;
    string httpMethod;
    map<string> requestContext;
    map<string> headers;
    map<string[]> multiValueHeaders;
    map<string>? queryStringParameters;
    map<string[]>? multiValueQueryStringParameters;
    map<string>? pathParameters;
    map<string>? stageVariables;
    string? body;
    boolean isBase64Encoded;
};

# DynamoDBStreamRecord
#
# + NewImage - PNewImage
# + Keys - Keys
# + SequenceNumber - SequenceNumber
# + StreamViewType - StreamViewType
# + SizeBytes - SizeBytes
# + OldImage - OldImage
public type DynamoDBStreamRecord record {
    map<json> Keys;
    map<json> NewImage?;
    map<json> OldImage?;
    string StreamViewType;
    string SequenceNumber;
    int SizeBytes;
};

# DynamoDBRecord
#
# + eventID - eventID
# + awsRegion - awsRegion
# + eventSourceARN - eventSourceARN
# + eventVersion - eventVersion
# + eventSource - @DynamoDBStreamRecord
# + eventName - eventName
# + dynamodb - @DynamoDBStreamRecord
public type DynamoDBRecord record {
    string eventID;
    string eventVersion;
    DynamoDBStreamRecord dynamodb;
    string awsRegion;
    string eventName;
    string eventSourceARN;
    string eventSource;
};

# DynamoDBEvent
#
# + Records - @DynamoDBEvent array
public type DynamoDBEvent record {
    DynamoDBRecord[] Records;
};

# SESCommonHeaders
#
# + from - from
# + to - to
# + returnPath - Return path
# + subject - subject
# + date - date
# + messageId - messageId
public type SESCommonHeaders record {
    string[] 'from;
    string[] to;
    string returnPath;
    string messageId;
    string date;
    string subject;
};

# NameValue
#
# + name - name
# + value - value
public type NameValue record {
    string name;
    string value;
};

# Simple Email Service Mail
#
# + headers - Parameter Description  
# + source - source
# + destination - destination
# + headersTruncated - headersTruncated
# + messageId - messageId
# + commonHeaders - commonHeaders
# + timestamp - timestamp
public type SESMail record {
    SESCommonHeaders commonHeaders;
    string 'source;
    string timestamp;
    string[] destination;
    NameValue[] headers;
    boolean headersTruncated;
    string messageId;
};

# Simple Email Service Verdict
#
# + status - status
public type SESVerdict record {
    string status;
};

# Simple Email Service Action
#
# + type - type
# + invocationType - invocationType
# + functionArn - functionArn
public type SESAction record {
    string 'type;
    string invocationType;
    string functionArn;
};

# Simple Email Service Receipt
#
# + spamVerdict - spamVerdict
# + processingTimeMillis - processingTimeMillis
# + virusVerdict - virusVerdict
# + recipients - recipients
# + action - action
# + spfVerdict - spfVerdict
# + dkimVerdict - dkimVerdict
# + timestamp - timestamp
public type SESReceipt record {
    string[] recipients;
    string timestamp;
    SESVerdict spamVerdict;
    SESVerdict dkimVerdict;
    int processingTimeMillis;
    SESAction action;
    SESVerdict spfVerdict;
    SESVerdict virusVerdict;
};

# Simple Email Service Element
#
# + mail - @SESMail record
# + receipt - @SESReceipt record
public type SESElement record {
    SESMail mail;
    SESReceipt receipt;
};

# Simple Email Service Record
#
# + ses - @SESElement record
# + eventVersion - event version
# + eventSource - event source
public type SESRecord record {
    string eventVersion;
    SESElement ses;
    string eventSource;
};

# Simple Email Service Event
#
# + Records - @SESRecord array
public type SESEvent record {
    SESRecord[] Records;
};

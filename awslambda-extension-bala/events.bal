# Represents S3 identity related details.
#
# + principalId - S3 principalId   
public type S3Identity record {
    string principalId;
};

# Represents S3 bucket related details.
#
# + name - S3 bucket name
# + arn - S3 bucket arn
# + ownerIdentity - S3 bucket owners identity
public type S3Bucket record {
    string name;
    S3Identity ownerIdentity;
    string arn;
};

# Represents S3 Object related details.
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

# Represents S3 element related details.
#
# + bucket - s3 bucket related details
# + s3SchemaVersion - s3SchemaVersion
# + configurationId - Configuration id
# + object - s3 object related details
public type S3Element record {
    string s3SchemaVersion;
    string configurationId;
    S3Bucket bucket;
    S3Object 'object;
};

# Represents S3 bucket notification related details.
#
# + s3 - s3 element related details
# + awsRegion - awsRegion the s3 bucket belongs to
# + eventVersion - version of the event
# + responseElements - responseElements
# + eventSource - eventSource of the triggered event
# + eventTime - invoked time of the event
# + requestParameters - requestParameters
# + eventName - name of the invoked name
# + userIdentity - identity of the user who invoked the event
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

# Represents S3 event details recieved from AWS when S3 notification is triggered.
#
# + Records - list of s3 event notifications
public type S3Event record {
    S3Record[] Records;
};

# Represents Amazon Simple Queue Service notification related details.
#
# + awsRegion - region of the SQS notification
# + messageAttributes - attributes of the message
# + eventSourceARN - arn of the event source
# + eventSource - eventSource of the triggered event
# + messageId - id of the message
# + receiptHandle - receipt handle of the message 
# + md5OfBody - md5 hash of the body
# + attributes - attributes
# + body - body of the notification
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

# Represents AWS SQS event details recieved from AWS when SQS notification is triggered.
#
# + Records - list of SQS event notifications
public type SQSEvent record {
    SQSRecord[] Records;
};

# Represents AWS API Gateway proxy request details recieved from AWS when gateway is triggered.
#
# + resource - resource of the request
# + path - path of the request
# + headers - headers of the request
# + pathParameters - path parameters of the request
# + isBase64Encoded - field to identify if the content is based 64 encoded
# + multiValueQueryStringParameters - multi value query string parameters
# + requestContext - request context
# + multiValueHeaders - multi value headers
# + httpMethod - http method of the request
# + queryStringParameters - query string parameters
# + stageVariables - stage variables
# + body - body of the request
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

# Represents DynamoDB Stream related details.
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

# Represents DynamoDB related details.
#
# + eventID - event id of the dynamodb notification
# + awsRegion - aws region the s3 bucket belongs to
# + eventSourceARN - arn of the event soruce
# + eventVersion - version of the event
# + eventSource - @DynamoDBStreamRecord
# + eventName - name of the event
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

# Represents DynamoDB event details recieved from AWS when DynamoDB notification is triggered.
#
# + Records - @DynamoDBEvent array
public type DynamoDBEvent record {
    DynamoDBRecord[] Records;
};

# Represents Simple Email Service related details.
#
# + from - the sender address of the mail
# + to - the reciever address of the mail
# + returnPath - return path of the mail
# + subject - subject of the mail
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

# Represents Simple Email Service name related details.
#
# + name - name
# + value - value
public type NameValue record {
    string name;
    string value;
};

# Represents Simple Email Service Mail related details.
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

# Represents Simple Email Service Verdict related details.
#
# + status - status
public type SESVerdict record {
    string status;
};

# Represents Simple Email Service Action related details.
#
# + type - type
# + invocationType - invocationType
# + functionArn - functionArn
public type SESAction record {
    string 'type;
    string invocationType;
    string functionArn;
};

# Represents Simple Email Service Receipt related details.
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

# Represents Simple Email Service Element related details.
#
# + mail - mail related details of the event
# + receipt - recipet related details of the event
public type SESElement record {
    SESMail mail;
    SESReceipt receipt;
};

# Represents Simple Email Service notification related details.
#
# + ses - details of the SES event
# + eventVersion - event version
# + eventSource - event source
public type SESRecord record {
    string eventVersion;
    SESElement ses;
    string eventSource;
};

# Represents Simple Email Service event details recieved from AWS when SES notification is triggered.
#
# + Records - record list of SES notifications
public type SESEvent record {
    SESRecord[] Records;
};

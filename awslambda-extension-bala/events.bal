# Represents the S3 identity related details.
#
# + principalId - S3 principal ID
public type S3Identity record {
    string principalId;
};

# Represents the S3 bucket related details.
#
# + name - S3 bucket name
# + arn - S3 bucket arn
# + ownerIdentity - S3 bucket owners identity
public type S3Bucket record {
    string name;
    S3Identity ownerIdentity;
    string arn;
};

# Represents the S3 object related details.
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

# Represents the S3 element related details.
#
# + bucket - S3 bucket related details
# + s3SchemaVersion - Version of the S3 schema
# + configurationId - Configuration ID
# + object - S3 object related details
public type S3Element record {
    string s3SchemaVersion;
    string configurationId;
    S3Bucket bucket;
    S3Object 'object;
};

# Represents the S3 bucket notification related details.
#
# + s3 - S3 element related details
# + awsRegion - the AWS region to which the S3 bucket belongs 
# + eventVersion - version of the event
# + responseElements - the response elements
# + eventSource - the source of the triggered event
# + eventTime - invoked time of the event
# + requestParameters - parameters of the request
# + eventName - name of the invoked event
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

# Represents the S3 event details received from AWS when the S3 notification is triggered.
#
# + Records - A list of S3 event notification records
public type S3Event record {
    S3Record[] Records;
};

# Represents the Amazon simple queue service notification related details.
#
# + awsRegion - region of the SQS notification
# + messageAttributes - attributes of the message
# + eventSourceARN - arn of the event source
# + eventSource - source of the triggered event
# + messageId - ID of the message
# + receiptHandle - receipt handle of the message 
# + md5OfBody - md5 hash of the body
# + attributes - the attributes associated with the queue
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

# Represents the AWS SQS event details received from AWS when the SQS notification is triggered.
#
# + Records - list of SQS event notifications
public type SQSEvent record {
    SQSRecord[] Records;
};

# Represents the AWS API Gateway proxy request details received from AWS when the gateway is triggered.
#
# + resource - the resource path defined in API Gateway
# + path - the url path for the caller
# + headers - headers of the request
# + pathParameters - path parameters of the request
# + isBase64Encoded - field to identify if the content is Base64 encoded
# + multiValueQueryStringParameters - multi value query string parameters that were part of the request
# + requestContext - request context of the request
# + multiValueHeaders - multi value headers if its enabled
# + httpMethod - HTTP method of the request
# + queryStringParameters - query string parameters that were part of the request
# + stageVariables - stage variables defined for the stage in API Gateway
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

# Represents the DynamoDB Stream related details.
#
# + NewImage - the item in the DynamoDB table as it appeared after it was modified
# + Keys - the primary key attribute(s) for the DynamoDB item that was modified
# + SequenceNumber - the sequence number of the stream record
# + StreamViewType - the type of data from the modified DynamoDB item that was captured in this stream record
# + SizeBytes - the size of the stream record, in bytes
# + OldImage - the item in the DynamoDB table as it appeared before it was modified
public type DynamoDBStreamRecord record {
    map<json> Keys;
    map<json> NewImage?;
    map<json> OldImage?;
    string StreamViewType;
    string SequenceNumber;
    int SizeBytes;
};

# Represents the DynamoDB related details.
#
# + eventID - a globally unique identifier for the event that was recorded in this stream record
# + awsRegion - AWS region to which the DynamoDB belongs to
# + eventSourceARN - the event source arn of DynamoDB
# + eventVersion - the version number of the stream record format
# + eventSource - the AWS service from which the stream record originated
# + eventName - the type of data modification that was performed on the DynamoDB table
# + dynamodb - the main body of the stream record, containing all of the DynamoDB-specific fields
public type DynamoDBRecord record {
    string eventID;
    string eventVersion;
    DynamoDBStreamRecord dynamodb;
    string awsRegion;
    string eventName;
    string eventSourceARN;
    string eventSource;
};

# Represents the DynamoDB event details received from AWS when the DynamoDB notification is triggered.
#
# + Records - record list of DynamoDB notifications
public type DynamoDBEvent record {
    DynamoDBRecord[] Records;
};

# Represents the simple email service related details.
#
# + from - the sender address of the mail
# + to - the reciever address of the mail
# + returnPath - return path of the mail
# + subject - subject of the mail
# + date - the day on which the mail was sent
# + messageId - the ID of the message
public type SESCommonHeaders record {
    string[] 'from;
    string[] to;
    string returnPath;
    string messageId;
    string date;
    string subject;
};

# Represents the simple email service name related details.
#
# + name - name of the header
# + value - value of the header
public type NameValue record {
    string name;
    string value;
};

# Represents the simple email service mail related details.
#
# + headers - Parameter Description  
# + source - source of the mail
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

# Represents the simple email service verdict related details.
#
# + status - status of the verdict
public type SESVerdict record {
    string status;
};

# Represents the simple email service action related details.
#
# + type - type of the email service action
# + invocationType - invocation type of the email service
# + functionArn - arn of the function thats executed
public type SESAction record {
    string 'type;
    string invocationType;
    string functionArn;
};

# Represents the simple email service receipt related details.
#
# + spamVerdict - object that indicates whether the message is spam
# + processingTimeMillis - processing time taken for the mail in milliseconds
# + virusVerdict - virus verdict status of the mail
# + recipients - a list of recipients that were matched by the active receipt rule
# + action - Object that encapsulates information about the action that was executed
# + spfVerdict - object that indicates whether the Sender Policy Framework (SPF) check passed
# + dkimVerdict - object that indicates whether the DomainKeys Identified Mail (DKIM) check passed
# + timestamp - string that specifies the date and time at which the action was triggered, in ISO 8601 format
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

# Represents the simple email service element related details.
#
# + mail - mail-related details of the event
# + receipt - recipet-related details of the event
public type SESElement record {
    SESMail mail;
    SESReceipt receipt;
};

# Represents the simple email service notification related details.
#
# + ses - details of the SES event
# + eventVersion - the version number of the mail format
# + eventSource - the AWS service from which the mail originated
public type SESRecord record {
    string eventVersion;
    SESElement ses;
    string eventSource;
};

# Represents the simple email service event details received from AWS when the SES notification is triggered.
#
# + Records - record list of the SES notifications
public type SESEvent record {
    SESRecord[] Records;
};

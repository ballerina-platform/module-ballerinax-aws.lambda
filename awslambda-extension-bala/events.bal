public type S3Identity record {
    string principalId;
};

public type S3Bucket record {
    string name;
    S3Identity ownerIdentity;
    string arn;
};

public type S3Object record {
    string key;
    int size;
    string eTag;
    string sequencer;
};

public type S3Element record {
    string s3SchemaVersion;
    string configurationId;
    S3Bucket bucket;
    S3Object 'object;
};

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

public type S3Event record {
    S3Record[] Records;
};

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

public type SQSEvent record {
    SQSRecord[] Records;
};

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

public type DynamoDBStreamRecord record {
    map<json> Keys;
    map<json> NewImage?;
    map<json> OldImage?;
    string StreamViewType;
    string SequenceNumber;
    int SizeBytes;
};

public type DynamoDBRecord record {
    string eventID;
    string eventVersion;
    DynamoDBStreamRecord dynamodb;
    string awsRegion;
    string eventName;
    string eventSourceARN;
    string eventSource;
};

public type DynamoDBEvent record {
    DynamoDBRecord[] Records;
};

public type SESCommonHeaders record {
    string[] 'from;
    string[] to;
    string returnPath;
    string messageId;
    string date;
    string subject;
};

public type NameValue record {
    string name;
    string value;
};

public type SESMail record {
    SESCommonHeaders commonHeaders;
    string 'source;
    string timestamp;
    string[] destination;
    NameValue[] headers;
    boolean headersTruncated;
    string messageId;
};

public type SESVerdict record {
    string status;
};

public type SESAction record {
    string 'type;
    string invocationType;
    string functionArn;
};

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

public type SESElement record {
    SESMail mail;
    SESReceipt receipt;
};

public type SESRecord record {
    string eventVersion;
    SESElement ses;
    string eventSource;
};

public type SESEvent record {
    SESRecord[] Records;
};
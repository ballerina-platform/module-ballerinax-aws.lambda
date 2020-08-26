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

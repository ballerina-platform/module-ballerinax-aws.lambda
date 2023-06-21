import ballerinax/aws.lambda;
public function main ()returns error? {
lambda:__register("echo",__func_proxy__echo,json );lambda:__register("uuid",__func_proxy__uuid,json );lambda:__register("ctxinfo",__func_proxy__ctxinfo,json );lambda:__register("notifySQS",__func_proxy__notifySQS,                          lambda:SQSEvent );lambda:__register("notifyS3",__func_proxy__notifyS3,                         lambda:S3Event );lambda:__register("notifyDynamoDB",__func_proxy__notifyDynamoDB,                               lambda:DynamoDBEvent );lambda:__register("notifySES",__func_proxy__notifySES,                          lambda:SESEvent );lambda:__register("apigwRequest",__func_proxy__apigwRequest,                             lambda:APIGatewayProxyRequest );lambda:__process();};public function __func_proxy__echo (lambda:Context ctx,anydata input)returns json {
return echo(ctx,<json >input);};public function __func_proxy__uuid (lambda:Context ctx,anydata input)returns json {
return uuid(ctx,<json >input);};public function __func_proxy__ctxinfo (lambda:Context ctx,anydata input)returns json|error {
return ctxinfo(ctx,<json >input);};public function __func_proxy__notifySQS (lambda:Context ctx,anydata input)returns json {
return notifySQS(ctx,<                          lambda:SQSEvent >input);};public function __func_proxy__notifyS3 (lambda:Context ctx,anydata input)returns json {
return notifyS3(ctx,<                         lambda:S3Event >input);};public function __func_proxy__notifyDynamoDB (lambda:Context ctx,anydata input)returns json {
return notifyDynamoDB(ctx,<                               lambda:DynamoDBEvent >input);};public function __func_proxy__notifySES (lambda:Context ctx,anydata input)returns json {
return notifySES(ctx,<                          lambda:SESEvent >input);};public function __func_proxy__apigwRequest (lambda:Context ctx,anydata input){
return apigwRequest(ctx,<                             lambda:APIGatewayProxyRequest >input);};

import ballerinax/awslambda;
public function main ()returns error? {
awslambda:__register("echo",__func_proxy__echo,json );awslambda:__register("uuid",__func_proxy__uuid,json );awslambda:__register("ctxinfo",__func_proxy__ctxinfo,json );awslambda:__register("notifySQS",__func_proxy__notifySQS,                          awslambda:SQSEvent );awslambda:__register("notifyS3",__func_proxy__notifyS3,                         awslambda:S3Event );awslambda:__register("notifyDynamoDB",__func_proxy__notifyDynamoDB,                               awslambda:DynamoDBEvent );awslambda:__register("notifySES",__func_proxy__notifySES,                          awslambda:SESEvent );awslambda:__register("apigwRequest",__func_proxy__apigwRequest,                             awslambda:APIGatewayProxyRequest );}public function __func_proxy__echo (awslambda:Context ctx,anydata input)returns json {
return echo(ctx,<json >input);}public function __func_proxy__uuid (awslambda:Context ctx,anydata input)returns json {
return uuid(ctx,<json >input);}public function __func_proxy__ctxinfo (awslambda:Context ctx,anydata input)returns json|error {
return ctxinfo(ctx,<json >input);}public function __func_proxy__notifySQS (awslambda:Context ctx,anydata input)returns json {
return notifySQS(ctx,<                          awslambda:SQSEvent >input);}public function __func_proxy__notifyS3 (awslambda:Context ctx,anydata input)returns json {
return notifyS3(ctx,<                         awslambda:S3Event >input);}public function __func_proxy__notifyDynamoDB (awslambda:Context ctx,anydata input)returns json {
return notifyDynamoDB(ctx,<                               awslambda:DynamoDBEvent >input);}public function __func_proxy__notifySES (awslambda:Context ctx,anydata input)returns json {
return notifySES(ctx,<                          awslambda:SESEvent >input);}public function __func_proxy__apigwRequest (awslambda:Context ctx,anydata input){
return apigwRequest(ctx,<                             awslambda:APIGatewayProxyRequest >input);}

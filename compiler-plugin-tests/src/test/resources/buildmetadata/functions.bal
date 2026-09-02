import ballerinax/aws.lambda;

@lambda:Function
public function echo(lambda:Context ctx, json input) returns json {
    return input;
}

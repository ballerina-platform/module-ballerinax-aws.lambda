import ballerinax/awslambda;
import ballerina/system;

@awslambda:Function
public function echo(awslambda:Context ctx, json input) returns json|error {
   return input;
}

@awslambda:Function
public function uuid(awslambda:Context ctx, json input) returns json|error {
   return system:uuid();
}


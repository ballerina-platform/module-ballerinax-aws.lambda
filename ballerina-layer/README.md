# Generating the Ballerina layer

- Copy the Ballerina distribution to the "ballerina-layer" directory
- Rename the Ballerina distribution directory to "ballerina"
- Create a zip file which contains the directory "ballerina" and the file "bootstrap", e.g. command "zip -r runtime.zip ballerina bootstrap"
- Upload the layer zip file to AWS S3, and create the AWS Lambda layer from the S3 link

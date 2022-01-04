Set up device as a new Thing in IoT Core and attach a policy that allows the device to receive from the 
‘rpi/input’ MQTT topic and publish to ‘rpi/output’ and ‘rpi/log’. It should also allow ‘iot:Connect’ for the 
client ID ‘EmeraldThumb’.

Download certs and keys into the EmeraldThumb folder in the device, in the same directory as emeraldThumb.py

Create an S3 bucket with 4  folders: app, data, raw, and photo

Create the first lambda function that processes MQTT data. Set a rule to trigger that uses the contents of 
a message published to rpi/output as the lambda's input event. Give this function S3 permissions to put new 
files into the 'data' and 'raw' folders

Create the second lambda function to republish S3 'app' folder uploads to rpi/input. Ensure this lambda has 
S3 get permissions and IoT publish permissions

Both lambda configurations can also be seen in their included .yaml files

I used Amplify CLI to create a Cognito identity pool and federated identity info. This can be done manually, 
however your information will need to be updated into the mobile app's amplifyconfiguration.json and 
awsconfiguration.json files. The auth role for your identities should include permissions to get and put 
objects in S3. More info on Amplify in appCode/ReadMe.txt
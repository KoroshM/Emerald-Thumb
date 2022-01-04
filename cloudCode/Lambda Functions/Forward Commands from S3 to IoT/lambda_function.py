import json
import boto3

iot = boto3.client('iot-data')
s3 = boto3.client('s3')

def lambda_handler(event, context):
    response = s3.get_object(Bucket="rpiet", Key="app/ETcommand.json")
    content = response['Body'].read().decode('utf-8')
    content = json.loads(content)
    
    iot.publish(
            topic = 'rpi/input',
            qos = 0,
            payload = bytes(json.dumps(content).encode('UTF-8'))
        )
    
    return
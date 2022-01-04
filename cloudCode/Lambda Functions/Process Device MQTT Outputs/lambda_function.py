import json
import boto3

s3 = boto3.client('s3')

def lambda_handler(event, context):
    # File name for the saved data
    filename = 'ETdata' + '.json'

    # Save raw data to rpiet bucket (Raspberry Pi Emerald Thumb)
    byteStream = bytes(json.dumps(event).encode('UTF-8'))
    s3.put_object(Bucket = 'rpiet', Key = 'raw/' + filename, Body = byteStream)
    
    # Process raw data into understandable information
    moisture = int(event['moist'])
    moistlevel = "Very Dry!"
    if(moisture > 900):
        moistlevel = "Drenched!"
    elif(moisture > 700):
        moistlevel = "Wet"
    elif(moisture > 450):
        moistlevel = "Moist"
    elif(moisture > 200):
        moistlevel = "Dry"
    
    temperature = float(event['temp'])
    templevel = "Freezing!"
    if(temperature > 30):
        templevel = "Hot!"
    elif(temperature > 27):
        templevel = "Warm"
    elif(temperature > 22):
        templevel = "Perfect"
    elif(temperature > 17):
        templevel = "Cool"
    elif(temperature > 0):
        templevel = "Cold!"
        
    water = float(event['wtr'])
    waterlevel = "Low!"
    if(water > 800):
        waterlevel = "Plenty!"
    elif(water > 400):
        waterlevel = "Good"
    elif(water > 200):
        waterlevel = "OK"
    
    # Save processed information with raw data into final file
    payload = {
        "moistlvl": moistlevel,
        "moistlvlraw": moisture,
        "templvl": templevel,
        "templvlraw": temperature,
        "waterlvl": waterlevel,
        "waterlvlraw": water
    }
    
    # Save processed data
    byteStream = bytes(json.dumps(payload).encode('UTF-8'))
    s3.put_object(Bucket = 'rpiet', Key = 'data/' + filename, Body = byteStream)
    
    return moistlevel
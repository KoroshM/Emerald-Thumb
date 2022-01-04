# Korosh Moosavi
# Fall 2021
# Emerald Thumb device software
#
# Configured via crontab to be executed in the background on startup
# Implements functions for device operation and communication
# Connects to AWS IoT Core and subscribes to rpi/input
# Outputs are published to rpi/output
# Basic logging is output to rpi/log
# More detailed logs available in this directory as ETlog.txt

from AWSIoTPythonSDK.MQTTLib import AWSIoTMQTTClient
from picamera import PiCamera
from adafruit_seesaw.seesaw import Seesaw
import relay
import datetime as dt
import subprocess
import board
import boto3
import logging
import time
import argparse
import json
import os

# Water level is saved to file for consistency across reboots
FILE = open(r"waterlevel.txt", "r+")
WATER_LEVEL = int(float(FILE.readline()))
# NOTE: Will lose 0.5mL ~50% of the time on reboot from test evaporation feature in main loop
FILE.close()

DISPENSE_AMOUNT_ML = 100
# Calculate seconds to leave the pump on for above amount
# Based on 100 mL/min flow rate
SECONDS = DISPENSE_AMOUNT_ML / 15

mqttout = "rpi/output"
mqttinput = "rpi/input"
mqttlog = "rpi/log"

# Restart the device
def reboot():
   os.system('sudo reboot')

# Dispense water
def water():
   global WATER_LEVEL
   global pump
   global SECONDS

   pump.on()
   time.sleep(SECONDS)
   pump.off()
   WATER_LEVEL -= DISPENSE_AMOUNT_ML
   
   FILE = open(r"waterlevel.txt", "r+")
   FILE.truncate(0)
   FILE.write(str(WATER_LEVEL))
   FILE.close()
   
# Set amount of water to dispense
def setwateringamount(value):
   global DISPENSE_AMOUNT_ML
   global SECONDS
   DISPENSE_AMOUNT_ML = value
   SECONDS = int(DISPENSE_AMOUNT_ML / 3 * 5)   

# Update the plant image
def updatePic():
   camera = PiCamera()
   camera.annotate_text = dt.datetime.now().strftime('%m/%d/%Y %H:%M:%S')
   camera.capture('plantpic.jpg')
   camera.close()
   cmd = "s3cmd put --config /home/pi/.s3cfg --acl-public %s/%s s3://rpiet/photo/%s" % (fileloc, filename, filename)
   subprocess.call(cmd, shell=True)
   print('Uploading photo update')

# Update measurements for temperature and moisture
def updateMetrics():
   global WATER_LEVEL
   global mqttout
   global myAWSIoTMQTTClient
   moisture = ss.moisture_read()
   temperature = ss.get_temp()

   payload = {
      "moist":str(moisture), 
      "temp":str(temperature),
      "wtr":str(WATER_LEVEL)
      }

   rpiJson = json.dumps(payload)                                  # Format message as JSON
   myAWSIoTMQTTClient.publish(mqttout, rpiJson, 0)                # Publish to topic
   print('Published to topic %s: %s\n' % (mqttout, payload))


# MQTT input format:
# {
#     "command": "string",
#     "value": int
# }
def customCallback(client, userdata, message):
   print("Received a new message from topic: " + message.topic)   # Display message received confirmation
   print(message.payload)
   global WATER_LEVEL
   global mqttlog
   global mqttout
   
   # Load input message
   msg = json.loads(message.payload)   
   action = msg['command']
   value = msg['value']
   
   # Publish received message confirmation
   myAWSIoTMQTTClient.publish(mqttlog, json.dumps({ "Received input": str(action) + ': ' + str(value) }), 0)    
   
   # Just a basic API
   if(action == "reboot"):                                        # Restart the device
      reboot()
   
   elif(action == "water"):                                       # Dispense water
      if(WATER_LEVEL > DISPENSE_AMOUNT_ML):
         water()
      else:
         myAWSIoTMQTTClient.publish(mqttlog, json.dumps({"Water level too low":str(WATER_LEVEL)}), 0)
         
   elif(action == "setamount"):                                   # Set amount of water to dispense
      if(value > 0):
         setwateringamount(value)
      else:
         myAWSIoTMQTTClient.publish(mqttlog, json.dumps(message.payload))
   
   elif(action == "updatewater"):                                 # Set a new water amount
      WATER_LEVEL_temp = int(value)
      if(WATER_LEVEL_temp > 0):
         WATER_LEVEL = WATER_LEVEL_temp
         FILE = open(r"waterlevel.txt", "r+")
         FILE.truncate(0)
         FILE.write(str(WATER_LEVEL))
         FILE.close()
      else:
         myAWSIoTMQTTClient.publish(mqttlog, json.dumps(message.payload))
   
   elif(action == "updateimage"):                                 # Take a new picture
      updatePic()
   
   elif(action == "updatestats"):                                 # Update metrics
      updateMetrics()
   
   elif(action == "updateall"):                                   # All-in-one update
      updatePic()
      updateMetrics()
      WATER_LEVEL_temp = int(value)                               # Value is used to set new water level
      if(WATER_LEVEL_temp > 0):
         WATER_LEVEL = WATER_LEVEL_temp
         FILE = open(r"waterlevel.txt", "r+")
         FILE.truncate(0)
         FILE.write(str(WATER_LEVEL))
         FILE.close()
      else:
         myAWSIoTMQTTClient.publish(mqttlog, json.dumps(message.payload))
   
   # Publish error if command not recognized
   else:                                                          
      myAWSIoTMQTTClient.publish(mqttlog, json.dumps(message.payload))         
   
   print("------Processed Input--------\n\n")                     # Print visual separator

# Set connection variables
host = "auix9tujc4lfn-ats.iot.us-west-2.amazonaws.com"
rootCAPath = "Amazon-root-CA-1.pem"
certificatePath = "device.pem.crt"
privateKeyPath = "private.pem.key"
port = 8883
clientId = "EmeraldThumb"

# Configure logging
logger = logging.getLogger("AWSIoTPythonSDK.core")
logger.setLevel(logging.DEBUG)
streamHandler = logging.StreamHandler()
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
streamHandler.setFormatter(formatter)
logger.addHandler(streamHandler)

# Init AWSIoTMQTTClient
myAWSIoTMQTTClient = AWSIoTMQTTClient(clientId)
myAWSIoTMQTTClient.configureEndpoint(host, port)
myAWSIoTMQTTClient.configureCredentials(rootCAPath, privateKeyPath, certificatePath)

# AWSIoTMQTTClient connection configuration
myAWSIoTMQTTClient.configureAutoReconnectBackoffTime(1, 32, 20)
myAWSIoTMQTTClient.configureOfflinePublishQueueing(-1)          # Infinite offline Publish queueing
myAWSIoTMQTTClient.configureDrainingFrequency(2)                # Draining: 2 Hz
myAWSIoTMQTTClient.configureConnectDisconnectTimeout(10)        # 10 sec
myAWSIoTMQTTClient.configureMQTTOperationTimeout(5)             # 5 sec

# Connect and subscribe to MQTT input topic for this device
myAWSIoTMQTTClient.connect()
myAWSIoTMQTTClient.subscribe(mqttinput, 1, customCallback)
time.sleep(2)

# Prepare monitoring resources
i2c_bus = board.I2C()
ss = Seesaw(i2c_bus, addr = 0x36)                               # Moisture sensor

s3 = boto3.client('s3', region_name='us-west-2')                # S3 uploader object
bucket = 'rpiet/photos/'
fileloc = '/home/pi/Desktop/EmeraldThumb'
filename = 'plantpic.jpg'

pump = relay.Relay(12, False)                                   # Water pump

# Just keep listening for messages
while True: 
   updatePic()
   updateMetrics()
    
   time.sleep(3600)                                            # Update once per hour

   WATER_LEVEL -= 1.5       # TEST EVAPORATION FEATURE (approx 2.2 mL/hr room temp in 10cm diameter cylinder)
   if(WATER_LEVEL < 0):
      WATER_LEVEL = 0
   FILE = open(r"waterlevel.txt", "r+")
   FILE.truncate(0)
   FILE.write(str(WATER_LEVEL))
   FILE.close()

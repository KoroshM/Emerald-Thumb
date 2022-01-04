The device is configured with the crontab function in cmd to run this script in the background 60 seconds after startup. The output of emeraldThumb.py is redirected to ETlog.txt. In cmd:

sudo crontab -e
@reboot sleep 60 && cd /home/pi/Desktop/EmeraldThumb && python3 emeraldThumb.py > ETlog.txt 2>&1


The EmeraldThumb folder on the device contains the certs, keys, plant photo, log text file, and water level text file
(in addition to the two python files included here)


***** MQTT Topics Used *****
Output = "rpi/output"
Input = "rpi/input"
MQTT Log = "rpi/log"


***** MQTT input format *****
{
   "command": "string",
   "value": int
}


***** Emerald Thumb input commands *****

"reboot"
Restart the device
   
   
"water"
Dispense water

         
"setamount" (value)
Set amount of water to dispense

   
"updatewater" (value)
Set a new water amount

   
"updateimage"
Take a new picture
   
   
"updatestats"
Update metrics (moisture + temp, no picture)
   
   
"updateall" (value)
All-in-One updater, calls every other update function
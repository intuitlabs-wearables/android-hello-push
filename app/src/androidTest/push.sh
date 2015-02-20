#!/bin/bash

# parameter 1 : Intuit Sender ID
# parameter 2 : UserID
# parameter 3 : local json file name

echo Sending Push Notification with the following content:
echo sender: $1
echo users: $2 
echo payload: `cat $3`

curl --data "sender=$1&users=$2&payload=`cat $3`" http://intuitwear.intuitlabs.com/SenderD2D/wearable/send

echo '..all done'

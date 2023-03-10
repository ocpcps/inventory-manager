#!/bin/bash
echo "File Systems":
df -h
echo "Current Directory:"
pwd
stat ${JAR_PATH}
echo "/app Contents is: And JAR PATH is ${JAR_PATH}"
ls /app
echo "${JAR_PATH} Contents:"
ls ${JAR_PATH}
echo "Moving to jar path: ${JAR_PATH}"
cd ${JAR_PATH}
pwd
echo "Listing"
ls

if [ ! -z ${XFS_DEVICE} ]; then
    echo "Creating XFS Device ${XFS_DEVICE}";
    mkfs.xfs ${XFS_DEVICE}
fi


while [ true ];
do
   if [ -f "${JAR_NAME}" ]; then
           /usr/bin/java -jar ${JAR_ARGS} ${JAR_NAME}
   else
      echo "Waiting: ${JAR_NAME}"
      sleep 1
   fi
done
@echo off
echo starting EAMAI services...

echo starting Backend service...
cd "C:\Users\uokar\Projects\EAMAI\backend\target"
start cmd /k java -jar backend-0.0.1-SNAPSHOT.jar

echo starting AI service...
cd "C:\Users\uokar\Projects\EAMAI\ai-ec-analytics\.venv\Scripts"
call activate
cd "C:\Users\uokar\Projects\EAMAI\ai-ec-analytics"
start cmd /k python main.py

start cmd /k lt --port 8080 --subdomain eamai

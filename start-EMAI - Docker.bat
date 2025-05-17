@echo off
echo Deploying EAMAI.
echo Exposing port 8080
start cmd /k lt --port 8080 --subdomain eamai

echo Building EAMAI Docker Container.
start cmd /k docker compose up
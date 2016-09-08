#include <StopWatch.h>

const int inputButton = 2;
const int inputDetector = 3;
const int debugLED = 4;

bool outState = false;
bool isDetectorStateOn = false;
unsigned long elapsed = 0;

StopWatch stopWatch20Sec;
StopWatch stopWatchDelay;

void setup() {
  Serial.begin(9600);
  pinMode(inputButton, INPUT_PULLUP);
  pinMode(inputDetector, INPUT_PULLUP);
  pinMode(debugLED, OUTPUT);
}

void loop() {
  
  // read the input pins:
  int buttonState = digitalRead(inputButton);
  int detectorState = digitalRead(inputDetector);
  if(buttonState == 0){
    if(!stopWatch20Sec.isRunning()){
      stopWatch20Sec.start();
      Serial.print("___");
      Serial.println("Start Timer");
    }
    if(!outState){ 
      outState=!outState;
      digitalWrite(debugLED, HIGH);
    }
  }else if(buttonState == 1 && outState){
    outState=!outState;
    digitalWrite(debugLED, LOW);
  }
  if(detectorState == 1 && isDetectorStateOn == false){
    isDetectorStateOn = true;
    elapsed = stopWatch20Sec.elapsed();
    Serial.print("___");
    Serial.print(elapsed);
    if(elapsed >= 10000) elapsed = elapsed % 10000;
    Serial.print("___");
    Serial.println(elapsed);
  }else if(detectorState == 0 && isDetectorStateOn == true){
    isDetectorStateOn = false;
  }
  
  //Serial.println(stopWatch20Sec.elapsed());
  delay(1);       
}

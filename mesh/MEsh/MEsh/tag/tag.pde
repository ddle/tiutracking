/*****************************************************************
 * Example tX Code for RFM12 Module                              *
 * by Dung Dang Le, Daniel Ferguson,                             *
 * This code is licensed under GPL v.2                           *
 *****************************************************************
 * Set up:                                                       *
 * RFM12B (REVISION B) ->  ATMEGA328                             *
 *              SDI    ->  MOSI (PB3)                            *
 *              SDO    ->  MISO (PB4)                            *
 *              SCK    ->  SCK  (PB5)                            *
 *              nSel   ->  SS   (PB2)                            *
 * LED -> PD5                                                    *
 *****************************************************************/
//#include <global.h>
//#include <rf12.h>
//
//#include <avr/io.h>
//#include <avr/interrupt.h>
//#include <avr/pgmspace.h>
//#include <avr/eeprom.h>
//#include <stdlib.h>
    
    
#include <RF12.h>
#include <Ports.h>

#define PAYLOAD_SIZE 6

//#define DETECTOR 1
//#define TAG 2
//#define BASE 3

// RECEIVING FORMAT 
// FROM TAG       : [TAG][TAG_ID][DATA] // E.G 0x0201 =  TAG#1
// MINUMUM 2 BYTES

// this will be retrieved eeprom in later development:
byte TagID = 7; // 1...30 only, 0 is not allow in this lib
byte MessageID=0;                

//char payload[]= "bumble beeeeeeeeeeeeeeeee...";
unsigned char payload[PAYLOAD_SIZE+1];

void setup()
{
	pinMode(5,OUTPUT);
	rf12_initialize(1, RF12_433MHZ, 33);
}

void loop()
{  
    rf12_recvDone();
    if (rf12_canSend()) 
    {     
		
       //payload[0] = '$';			//Start1
	payload[0] = 0;				//Source ID
	payload[1] = 0;				//Detector ID		
	payload[2] = 0;				//RSSI value - we are the tag, we don't know this value.
	payload[3] = TagID;			//Tag ID
	payload[4] = MessageID;		        //Message ID	
	payload[5] = 33;			//Reserved - put battery level here
	MessageID++;		
	rf12_sendStart(0, payload, PAYLOAD_SIZE);
		
        delay(300);
    }
	   
}



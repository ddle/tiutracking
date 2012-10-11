/*****************************************************************
 * Example base Code using RFM12 Module                          *
 * by Dung Dang Le, Daniel Ferguson,                             *
 * This code is licensed under GPL v.2                           *
 *****************************************************************
 * Set up:                                                       *
 * RFM12B (REVISION B) ->  ATMEGA328                             *
 *              SDI    ->  MOSI (PB3)                            *
 *              SDO    ->  MISO (PB4)                            *
 *              SCK    ->  SCK  (PB5)                            *
 *              nSel   ->  SS   (PB2)                            *
 *              IRQ    ->  INT0 (PD2)                            *
 * ARSSI (at resistor) ->  PC0  (analog input 0)                 *
 *              VCC    ->  AVCC                                  *
 *****************************************************************/
#include <RF12.h>
#include <Ports.h>

#define FROM_TAG 		0
#define PAYLOAD_SIZE 	        6
#define MyID			255

#define FASTADC 1
// defines for setting and clearing register bits
#ifndef cbi
#define cbi(sfr, bit) (_SFR_BYTE(sfr) &= ~_BV(bit))
#endif
#ifndef sbi
#define sbi(sfr, bit) (_SFR_BYTE(sfr) |= _BV(bit))
#endif

unsigned char payload[PAYLOAD_SIZE+1];

//
void setup () 
{
#if FASTADC
  // set prescale to 16
  sbi(ADCSRA,ADPS2) ;
  cbi(ADCSRA,ADPS1) ;
  cbi(ADCSRA,ADPS0) ;
#endif
  Serial.begin(19200);   
  pinMode(5,OUTPUT);
  rf12_initialize(3, RF12_433MHZ,33);
}

void loop () 
{    
  if (rf12_recvDone() && rf12_crc == 0) 
  {
    //Serial.print("rf12_len=");
    //Serial.print(rf12_len,DEC);
    //Serial.println();
    //TODO: check to see if incoming message checksum is valid.
    //      and skip this if it is invalid.

    //TODO: if source ID doesn't have an entry in my routing table
    //		then ignore message.
    //foreach entry in routingTable
    //	if rf12_data[1] == entry then
    //		accept=true
    //endfor
    
    //If From Detector, then just relay received data.
    //If from Tag, then relay payload 

      if (rf12_data[0] != FROM_TAG)//From Detector
      {
       // payload[0] = 13;		//Start Delimiter
        payload[0] = MyID;			//Source ID
        payload[1] = rf12_data[1];		//Detector ID        
        payload[2] = rf12_data[2];		//RSSI value 
        payload[3] = rf12_data[3];		//Tag ID
        payload[4] = rf12_data[4];		//Message ID 
        payload[5] = rf12_data[5];		//Reserved
       
        // debug        
//        Serial.print("from detector:");
//        Serial.print(payload[1],DEC);
//        Serial.print(" tag:");
//        Serial.print(payload[3],DEC);
//        Serial.print(" has RSSI:");
//        Serial.print(payload[2],DEC);
//        Serial.println();    
      }
      else if (rf12_data[0] == FROM_TAG)//From Tag	
      {		
        // get RSSI
        int rssi = readRSSI()/2;            
        payload[0] = MyID;				//Source ID
        payload[1] = MyID;				//Detector ID        
        payload[2] = (unsigned char)rssi;		//RSSI value - we are the tag, we don't know this value.
        payload[3] = rf12_data[3];		        //Tag ID
        payload[4] = rf12_data[4];		        //Message ID 
        payload[5] = rf12_data[5];			//Reserved - put batter level here
       
       // debug        
//        Serial.print("tag:");
//        Serial.print(payload[3],DEC);
//        Serial.print(" has RSSI:");
//        Serial.print(payload[2],DEC);
//        Serial.println();        
        
      } 
        // to proxy
      Serial.print(payload[0],BYTE);
      Serial.print(payload[1],BYTE);
      Serial.print(payload[2],BYTE);
      Serial.print(payload[3],BYTE);
      Serial.print(payload[4],BYTE);
      Serial.print(payload[5],BYTE);
    }
}







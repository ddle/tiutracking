/*****************************************************************
 * Example detector Code using RFM12 Module                      *
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
<<<<<<< .mine
#define PAYLOAD_SIZE 	        8
#define MyID			10
=======
#define PAYLOAD_SIZE 	        6
#define MyID			8
>>>>>>> .r54

#define FASTADC 1

// defines for setting and clearing register bits
#ifndef cbi
#define cbi(sfr, bit) (_SFR_BYTE(sfr) &= ~_BV(bit))
#endif
#ifndef sbi
#define sbi(sfr, bit) (_SFR_BYTE(sfr) |= _BV(bit))
#endif

unsigned char payload[PAYLOAD_SIZE+1];
int tagMessageReceived=0;
int detMessageReceived=0;


void setup () 
{
  // quote:
  // The ADC clock is 16 MHz divided by a prescale factor.
  // The prescale is set to 128 (16MHz/128 = 125 KHz) in wiring.c.
  // Since a conversion takes 13 ADC clocks, 
  // the sample rate is about 125KHz/13 or 9600 Hz.
  // ...prescale of 16 give an ADC clock of 1 MHz and a sample 
  // rate of ~77KHz without much loss of resolution(!?)
  // note: we are using 8MHz internal clk
 
#if FASTADC
  // set prescale to 16
  sbi(ADCSRA,ADPS2) ;
  cbi(ADCSRA,ADPS1) ;
  cbi(ADCSRA,ADPS0) ;
#endif

  Serial.begin(19200);   
  pinMode(5,OUTPUT);
  rf12_initialize(2, RF12_433MHZ,33);
}

void loop () 
{      
  if (rf12_recvDone() && rf12_crc==0) 
  {
    //TODO: check to see if incoming message checksum is valid.
    //      and skip this if it is invalid.

    //TODO: if source ID doesn't have an entry in my routing table
    //		then ignore message.
    //foreach entry in routingTable
    //	if rf12_data[1] == entry then
    //		accept=true
    //endfor

    
    if (0)//rf12_data[0] != FROM_TAG)//From Detector
    {
      //payload[0] = rf12_data[0];		//Start Delimiter
      payload[0] = MyID;			//Source ID
      payload[1] = rf12_data[1];		//Detector ID      
      payload[2] = rf12_data[2];		//RSSI value 
      payload[3] = rf12_data[3];		//Tag ID
      payload[4] = rf12_data[4];		//Message ID 
      payload[5] = rf12_data[5];		//Reserved      
      //detMessageReceived=1;
      digitalWrite(5,HIGH);
      delay(random(200)+50);   
      rf12_sendStart(0, payload, PAYLOAD_SIZE);                
      digitalWrite(5,LOW);      
    }
    else if (rf12_data[0] == FROM_TAG)//From Tag
    {		

      // get RSSI        
      int rssi = readRSSI()/2;        
      payload[0] = MyID;			//Source ID
      payload[1] = MyID;			//Detector ID      
      payload[2] = (unsigned char)rssi;		//RSSI value - we are the tag, we don't know this value.
      payload[3] = rf12_data[3];		//Tag ID
      payload[4] = rf12_data[4];		//Message ID 
      payload[5] = rf12_data[5];		//Reserved      
      //tagMessageReceived=1;
      digitalWrite(5,HIGH);
      delay(random(200)+50);   
      rf12_sendStart(0, payload, PAYLOAD_SIZE);                
      digitalWrite(5,LOW);
    }  
  }

//  if ( ( tagMessageReceived  ) )
//  {
//    while (!rf12_canSend()){rf12_recvDone();}
//    digitalWrite(5,HIGH);
//    delay(random(200));
//    rf12_sendStart(0, payload, PAYLOAD_SIZE);
//         
//    //Serial.print("From Tag\r\n");        
//    tagMessageReceived=0;
//    
//
//    //for (int y = 0;y < 8;y++){
//     Serial.print(payload[4],DEC);
//     //Serial.print(", "); 
//     //}
//     Serial.println();		
//    digitalWrite(5,LOW);
//  }
}








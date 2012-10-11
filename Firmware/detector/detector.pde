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


#define FROM_TAG 		0
#define PAYLOAD_SIZE 	        8
#define MyID			7

#define FASTADC 1
#define CACHE_SIZE              100
#define CACHE_ENTRY_EMPTY        -1
#define TIME_DET_SLOT_DELTA          30
#define TIME_TAG_SLOT_DELTA          15
// defines for setting and clearing register bits
#ifndef cbi
#define cbi(sfr, bit) (_SFR_BYTE(sfr) &= ~_BV(bit))
#endif
#ifndef sbi
#define sbi(sfr, bit) (_SFR_BYTE(sfr) |= _BV(bit))
#endif

unsigned char payload[PAYLOAD_SIZE];

class CacheEntry{
  public:
  int did;
  int tid;
  int mid;
};

CacheEntry cache[CACHE_SIZE];

void setup () 
{
  for (int i = 0;i < CACHE_SIZE;++i){
    cache[i].did = 0;
    cache[i].tid = 0;
    cache[i].mid = -1;
  }
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



int key(int did, int tid){
 return ((tid & 0xff)<<8)+(did & 0xff); 
}

//Returns:
//  -1 is entryExists
//  otherwise, return the index of the slot in the cache we can use.
int entryExists(int did, int tid, int mid){
  for (int index = 0;index <  CACHE_SIZE;++index){
   if (key(did,tid) == key(cache[index].did,cache[index].tid)){
     //This detector has sent us a message before    
     if (abs(cache[index].mid - mid)>20){
       // there is a large gap between message Id's, assume we need to broadcast.
       return index;
     }
     else{
      if (mid > cache[index].mid){
        //but we have not sent a message with this mid before(Do broadcast)

       return index; 
      }else if (mid <= cache[index].mid){
        //but we've already sent a message with this mid(don't broadcast)
       return -1; 
      }
     }
   } 
  }
  //We have never recieved a message from this detector before.
  //so, find first empty slot, denoted by all -1's
  for (int index = 0;index <  CACHE_SIZE;++index){
    if (cache[index].did ==-1){
        return index;//is the next available slot.
    }
  }
}

void insertCache(int index, int did, int tid, int mid){
    cache[index].did = did;
    cache[index].tid = tid;
    cache[index].mid = mid; 
}

int timeDelayFromDet_ms(int tagId){
  return TIME_DET_SLOT_DELTA*tagId+random(30);
}

int timeDelayFromTag_ms(int tagId){
  return TIME_TAG_SLOT_DELTA*tagId+random(20);
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

    
    if (0){//rf12_data[0] != FROM_TAG)//From Detector

      //payload[0] = rf12_data[0];		//Start Delimiter
      payload[0] = MyID;			//Source ID
      payload[1] = rf12_data[1];		//Detector ID      
      payload[2] = rf12_data[2];		//HIGH BYTE - RSSI value 
      payload[3] = rf12_data[3];		//LOW BYTE - RSSI value 
      payload[4] = rf12_data[4];		//Tag ID
      payload[5] = rf12_data[5];		//Message ID 
      payload[6] = rf12_data[6];		//Tag Battery level
      payload[7] = rf12_data[7];                //Detector Battery level

      int index = entryExists(payload[1], payload[4],payload[5]);
      if (index != -1){
        insertCache(index,payload[1], payload[4],payload[5]);       
        delay(timeDelayFromDet_ms(MyID));  
        digitalWrite(5,HIGH); 
        delay(10);
        rf12_sendStart(0, payload, PAYLOAD_SIZE);                
        digitalWrite(5,LOW);
      }//Else we already broadcasted, do nothing.
      
           
    }
    else if (rf12_data[0] == FROM_TAG)//From Tag
    {
      // get RSSI        
      int rssi = readRSSI();        
      payload[0] = MyID;			        //Source ID
      payload[1] = MyID;			        //Detector ID      
      payload[2] = (unsigned char)((rssi & 0xFF00)>>8);	//HIGH BYTE - RSSI value - we are the tag, we don't know this value.
      payload[3] = (unsigned char)(rssi & 0x00FF);	//LOW BYTE - RSSI value - we are the tag, we don't know this value.
      payload[4] = rf12_data[4];		        //Tag ID
      payload[5] = rf12_data[5];		        //Message ID 
      payload[6] = rf12_data[6];		        //Tag Battery level      
      payload[7] = rf12_lowBat();                       //Detector Battery level
      
      //int index = entryExists(MyID, payload[4],payload[5]);
      //if (index != -1){
      //  insertCache(index,MyID, payload[4],payload[5]);
        
        delay(timeDelayFromTag_ms(MyID));   
        digitalWrite(5,HIGH);
        delay(10);
        rf12_sendStart(0, payload, PAYLOAD_SIZE);                
        digitalWrite(5,LOW);
      //}
    }  
  }
}








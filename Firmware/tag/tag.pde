//****************************************************************
/* Tag - Low power consumption
 * Dung Dang Le, Daniel Ferguson. 2011
 * reference source: Martin Nawrath - "The Nightingale" 
 * 
 * + Normal operating mode: 30mA when transmitting, 3mA when standby (delay)
 * + Tag saves its battery power by (after transmitting data)
 *   - put radio at sleep using jeelabs library function rf12_sleep()
 *   - put MCU at sleep using sleep() function provided by avr lib
 *   - wake up call : watchdog timer interrupt
 *****************************************************************
 * Set up:                                                       
 * RFM12B (REVISION B) ->  ATMEGA328                             
 *              SDI    ->  MOSI (PB3)                            
 *              SDO    ->  MISO (PB4)                            
 *              SCK    ->  SCK  (PB5)                            
 *              nSel   ->  SS   (PB2)                            
 *              IRQ    ->  INT0 (PD2)                         
 *              VCC    ->  AVCC
 *****************************************************************/

#include <RF12.h>
#include <avr/sleep.h>
#include <avr/wdt.h>

#ifndef cbi
#define cbi(sfr, bit) (_SFR_BYTE(sfr) &= ~_BV(bit))
#endif
#ifndef sbi
#define sbi(sfr, bit) (_SFR_BYTE(sfr) |= _BV(bit))
#endif

volatile boolean f_wdt=1;
byte radioIsOn = 1;

#define PAYLOAD_SIZE 8

byte TagID = 5; 
byte MessageID = 0;                

//char payload[]= "bumble beeeeeeeeeeeeeeeee...";
unsigned char payload[PAYLOAD_SIZE];

void setup(){

  pinMode(3,OUTPUT);
  rf12_initialize(1, RF12_433MHZ, 33);
  // CPU Sleep Modes 
  // SM2 SM1 SM0 Sleep Mode
  // 0    0  0 Idle
  // 0    0  1 ADC Noise Reduction
  // 0    1  0 Power-down
  // 0    1  1 Power-save
  // 1    0  0 Reserved
  // 1    0  1 Reserved
  // 1    1  0 Standby(1)

  cbi( SMCR,SE );      // sleep enable, power down mode
  cbi( SMCR,SM0 );     // power down mode
  sbi( SMCR,SM1 );     // power down mode
  cbi( SMCR,SM2 );     // power down mode

  // 0=16ms, 1=32ms,2=64ms,3=128ms,4=250ms,5=500ms
  // 6=1 sec,7=2 sec, 8=4 sec, 9= 8sec
  //random between (0 to 3) + 6
  setup_watchdog(6);
}


//****************************************************************
//****************************************************************
void loop()
{

    // wait for timed out watchdog / flag is set when a watchdog timeout occurs
    f_wdt=0;       // reset flag

    //  broadcasting data
    rf12_recvDone();
    if (rf12_canSend()) 
    {       		
      payload[0] = 't';				//Source ID
      payload[1] = 'a';				//Detector ID		
      payload[2] = 'g';				//HIGH BYTE - RSSI value - we are the tag, we don't know this value.
      payload[3] = 0;				//LOW BYTE - RSSI value - we are the tag, we don't know this value.
      payload[4] = TagID;			//Tag ID
      payload[5] = MessageID;		        //Message ID	
      payload[6] = rf12_lowBat();               //Tag battery level
      payload[7] = 0;                           //reserved for Detector battery level
      MessageID++;
      digitalWrite(3,HIGH);		
      rf12_sendStart(0, payload, PAYLOAD_SIZE);
      // see http://jeelabs.net/projects/cafe/wiki/POF_03_Wireless_light_sensor

      delay(20);           // wait for transmision completes
                          // this value depends on the baudrate of radio and amount of bytes broadcasted
      
      if (radioIsOn) 
      {
        rf12_sleep(0);    // turn the radio off
        radioIsOn = 0; 
        //digitalWrite(5,LOW);
      }
      digitalWrite(3,LOW);
      system_sleep();     // deep sleep: MCU + radio, wake up after amount of time specified
      delay(2);           // for debugging, this will show bump of 3mA on scope

      if (!radioIsOn)
      {
        rf12_sleep(-1);   // turn the radio back on
        radioIsOn = 1;
        //digitalWrite(5,HIGH);   
      }
      delay(2);           // for debugging, this will show bump of 3mA on scope
    }
}
//****************************************************************  
//****************************************************************  

// set system into the sleep state 
// system wakes up when wtchdog is timed out
void system_sleep() {

  cbi(ADCSRA,ADEN);                    // switch Analog to Digitalconverter OFF

  set_sleep_mode(SLEEP_MODE_PWR_DOWN); // sleep mode is set here
  sleep_enable();

  sleep_mode();                        // System sleeps here

  sleep_disable();                     // System continues execution here when watchdog timed out 
  sbi(ADCSRA,ADEN);                    // switch Analog to Digitalconverter ON

}

//****************************************************************
// attach watchdog timer interrpt for wake-up call
void setup_watchdog(int ii) {

  byte bb;
  int ww;
  if (ii > 9 ) ii=9;
  bb=ii & 7;
  if (ii > 7) bb|= (1<<5);
  bb|= (1<<WDCE);
  ww=bb;
  // Serial.println(ww);

  MCUSR &= ~(1<<WDRF);
  // start timed sequence
  WDTCSR |= (1<<WDCE) | (1<<WDE);
  // set new watchdog timeout value
  WDTCSR = bb;
  WDTCSR |= _BV(WDIE);
}
//****************************************************************  
// Watchdog Interrupt Service / is executed when  watchdog timed out
ISR(WDT_vect) {
  f_wdt=1;  // set global flag
}



/*****************************************************************
 * Detector Low power consumption code                           *
 * by Dung Dang Le, Daniel Ferguson,                             *
 * This code is licensed under GPL v.2                           *
 *                                                               *
 * reference source:                                             *
 * http://www.arduino.cc/playground/Learning/ArduinoSleepCode    *
 * 2006 MacSimski 2006-12-30                                     *
 * 2007 D. Cuartielles 2007-07-08 - Mexico DF                    * 
 *                                                               *
 * Detector saves its battery power by:                          *
 
 * 
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
 /* Reference notes:
  * what will make pin2 go LOW activating INT0 external interrupt, bringing
  * the MCU back to life
  * NOTE: when coming back from POWER-DOWN mode, it takes a bit
  *       until the system is functional at 100%!! (typically <1sec)
  * DDLE: WILL BE TESTED 
  *
  * functions provided in avr/power.h to disable other hardware modules:
  * power_adc_disable(),power_spi_disable(),power_timer0_disable(), 
  * power_timer1_disable(),power_timer2_disable(),power_twi_disable() power_usart0_disable
  */

 
#include <RF12.h>
#include <Ports.h>
#include <avr/sleep.h>
#include <avr/power.h>
//************************************************************
// Detector properties
#define FROM_TAG 		0
#define PAYLOAD_SIZE 	        6
#define MyID			2

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

//*****************************************************************
// sleep variables and functions
int wakePin = 2;                 // pin used for waking up
int sleepStatus = 0;             // variable to store a request for sleep
//int count = 0;                   // counter

void power_down()         
{
    /* Now is the time to set the sleep mode. In the Atmega8 datasheet
     * http://www.atmel.com/dyn/resources/prod_documents/doc2486.pdf on page 35
     * there is a list of sleep modes which explains which clocks and 
     * wake up sources are available in which sleep mode.
     *
     * In the avr/sleep.h file, the call names of these sleep modes are to be found:
     *
     * The 5 different modes are:
     *     SLEEP_MODE_IDLE         -the least power savings 
     *     SLEEP_MODE_ADC
     *     SLEEP_MODE_PWR_SAVE
     *     SLEEP_MODE_STANDBY
     *     SLEEP_MODE_PWR_DOWN     -the most power savings
     *
     * For now, we want as much power savings as possible, so we 
     * choose the according 
     * sleep mode: SLEEP_MODE_PWR_DOWN
     * 
     */  
     
    set_sleep_mode(SLEEP_MODE_IDLE);   // sleep mode is set here

    sleep_enable();          // enables the sleep bit in the mcucr register
                             // so sleep is possible. just a safety pin 

    /* Now it is time to enable an interrupt. We do it here so an 
     * accidentally pushed interrupt button doesn't interrupt 
     * our running program. 
     */
    
    power_adc_disable();
    //power_spi_disable();
    //power_timer0_disable();
    power_timer1_disable();
    power_timer2_disable();
    power_twi_disable() ;
    power_usart0_disable();
    
    sleep_mode();            // here the device is actually put to sleep!!
                             // THE PROGRAM CONTINUES FROM HERE AFTER WAKING UP

    power_adc_enable();
    //power_spi_enable();
   // power_timer0_enable();
    power_timer1_enable();
    power_timer2_enable();
    power_twi_enable() ;
    power_usart0_enable();


    sleep_disable();         // first thing after waking from sleep:
                             // disable sleep...
   // detachInterrupt(0);    // disables interrupt 0 on pin 2 so the 
                             // wakeUpNow code will not be executed 
                             // during normal running time.
                        
}

void sleepNow()         
{
    /* Now is the time to set the sleep mode. In the Atmega8 datasheet
     * http://www.atmel.com/dyn/resources/prod_documents/doc2486.pdf on page 35
     * there is a list of sleep modes which explains which clocks and 
     * wake up sources are available in which sleep mode.
     *
     * In the avr/sleep.h file, the call names of these sleep modes are to be found:
     *
     * The 5 different modes are:
     *     SLEEP_MODE_IDLE         -the least power savings 
     *     SLEEP_MODE_ADC
     *     SLEEP_MODE_PWR_SAVE
     *     SLEEP_MODE_STANDBY
     *     SLEEP_MODE_PWR_DOWN     -the most power savings
     *
     * For now, we want as much power savings as possible, so we 
     * choose the according 
     * sleep mode: SLEEP_MODE_PWR_DOWN
     * 
     */  
     
    set_sleep_mode(SLEEP_MODE_IDLE);   // sleep mode is set here

    sleep_enable();          // enables the sleep bit in the mcucr register
                             // so sleep is possible. just a safety pin 

    /* Now it is time to enable an interrupt. We do it here so an 
     * accidentally pushed interrupt button doesn't interrupt 
     * our running program. 
     */
    
    sleep_mode();            // here the device is actually put to sleep!!
                             // THE PROGRAM CONTINUES FROM HERE AFTER WAKING UP

    sleep_disable();         // first thing after waking from sleep:
                             // disable sleep...
   // detachInterrupt(0);    // disables interrupt 0 on pin 2 so the 
                             // wakeUpNow code will not be executed 
                             // during normal running time.
                        
}


//************************************************************
void setup () 
{
  pinMode(wakePin, INPUT); // use PD2 as wake up pin, PD2 is connected to IRQ pin of RF12
                           // this means MCU will be waked up when there are some valid data on air

  /*  
   * attachInterrupt(A, B, C)
   * A   can be either 0 or 1 for interrupts on pin 2 or 3.   
   * 
   * B   Name of a function you want to execute while in interrupt A.
   *
   * C   Trigger mode of the interrupt pin. can be:
   *             LOW        a low level trigger
   *             CHANGE     a change in level trigger
   *             RISING     a rising edge of a level trigger
   *             FALLING    a falling edge of a level trigger
   *
   * In all but the IDLE sleep modes only LOW can be used.
   */

  // attachInterrupt(0, wakeUpNow, LOW); // use interrupt 0 (pin 2) and run function
                                        // wakeUpNow when pin 2 gets LOW 
  // notice that the RF12.cpp already done this line 384: attachInterrupt(0, rf12_interrupt, LOW);
  
  // note: we are using 8MHz internal clk
  #if FASTADC
  // set prescale to 16
  sbi(ADCSRA,ADPS2) ;
  cbi(ADCSRA,ADPS1) ;
  cbi(ADCSRA,ADPS0) ;
  #endif

  //Serial.begin(19200);   
  pinMode(5,OUTPUT);
  rf12_initialize(2, RF12_433MHZ,33);
}

//*****************************************************************
void loop () 
{     
  power_down(); 
  //sleepNow();              // sleep function called here
                           // radio is still listenning
                           // whenever it feels ready, it will interrupt MCU
                           // rf12_interrupt() will be triggered 
  //delay(500);             // waits for a second ??? for MCU ready
  broadcast();             // transmit received data
}

void broadcast()
{
  if (rf12_recvDone() && rf12_crc==0) 
  {  
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
}





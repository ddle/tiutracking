// RFM12B driver implementation
// 2009-02-09 <jcw@equi4.com> http://opensource.org/licenses/mit-license.php
// $Id: RF12.cpp 6019M 2010-11-18 03:45:19Z (local) $

#include "RF12.h"
#include <avr/io.h>
#include <util/crc16.h>
#include <avr/eeprom.h>
#include <avr/sleep.h>
#include <WProgram.h>

// maximum transmit / receive buffer: 3 header + data + 2 crc bytes
#define RF_MAX   (RF12_MAXDATA + 5)

// pins used for the RFM12B interface
#if defined(__AVR_ATmega1280__)

#define RFM_IRQ     2
#define SS_PORT     PORTB
#define SS_BIT      0
#define SPI_SS      53
#define SPI_MOSI    51
#define SPI_MISO    50
#define SPI_SCK     52

#elif defined(__AVR_ATtiny84__)

#define RFM_IRQ     2
#define SS_PORT     PORTA
#define SS_BIT      7
#define SPI_SS      3
#define SPI_MISO    5
#define SPI_MOSI    4
#define SPI_SCK     6

#else

// ATmega328, etc.
#define RFM_IRQ     2
#define SS_PORT     PORTB
#define SS_BIT      2
#define SPI_SS      10
#define SPI_MOSI    11
#define SPI_MISO    12
#define SPI_SCK     13

#endif 

// RF12 command codes
#define RF_RECEIVER_ON  0x82DD
#define RF_XMITTER_ON   0x823D
#define RF_IDLE_MODE    0x820D
#define RF_SLEEP_MODE   0x8205
#define RF_WAKEUP_MODE  0x8207
#define RF_TXREG_WRITE  0xB800
#define RF_RX_FIFO_READ 0xB000
#define RF_WAKEUP_TIMER 0xE000

// RF12 status bits
#define RF_LBD_BIT      0x0400
#define RF_RSSI_BIT     0x0100

// bits in the node id configuration byte
#define NODE_BAND       0xC0        // frequency band
#define NODE_ACKANY     0x20        // ack on broadcast packets if set
#define NODE_ID         0x1F        // id of this node, as A..Z or 1..31
									// DDLE: 5 bits to be ORed with id later
// transceiver states, these determine what to do with each interrupt
enum {
    TXCRC1, TXCRC2, TXTAIL, TXDONE, TXIDLE,
    TXRECV,
    TXPRE1, TXPRE2, TXPRE3, TXSYN1, TXSYN2,
};

static uint8_t nodeid;              // address of this node
static uint8_t group;               // network group
static volatile uint8_t rxfill;     // number of data bytes in rf12_buf
static volatile int8_t rxstate;     // current transceiver state

#define RETRIES     8               // stop retrying after 8 times
#define RETRY_MS    1000            // resend packet every second until ack'ed

static uint8_t ezInterval;          // number of seconds between transmits
static uint8_t ezSendBuf[RF12_MAXDATA]; // data to send
static char ezSendLen;              // number of bytes to send
static uint8_t ezPending;           // remaining number of retries
static long ezNextSend[2];          // when was last retry [0] or data [1] sent

volatile uint16_t rf12_crc;         // running crc value
volatile uint8_t rf12_buf[RF_MAX];  // recv/xmit buf, including hdr & crc bytes
long rf12_seq;                      // seq number of encrypted packet (or -1)

int RSSI = 0;						// DDL: received signal strength 
int count = 0;

static uint32_t seqNum;             // encrypted send sequence number
static uint32_t cryptKey[4];        // encryption key to use
void (*crypter)(uint8_t);           // does en-/decryption (null if disabled)



static void spi_initialize () {
    digitalWrite(SPI_SS, 1);
    pinMode(SPI_SS, OUTPUT);
    pinMode(SPI_MOSI, OUTPUT);
    pinMode(SPI_MISO, INPUT);
    pinMode(SPI_SCK, OUTPUT);
#ifdef SPCR    
#if F_CPU <= 10000000
    // clk/4 is ok for the RF12's SPI
    SPCR = _BV(SPE) | _BV(MSTR);
#else
    // use clk/8 (2x 1/16th) to avoid exceeding RF12's SPI specs of 2.5 MHz
    SPCR = _BV(SPE) | _BV(MSTR) | _BV(SPR0);
    SPSR |= _BV(SPI2X);
#endif
#else
    // ATtiny
    USICR = bit(USIWM0);
#endif
}

static uint8_t rf12_byte (uint8_t out) {
#ifdef SPDR
    SPDR = out;
    // this loop spins 4 usec with a 2 MHz SPI clock
    while (!(SPSR & _BV(SPIF)))
        ;
    return SPDR;
#else
    // ATtiny
    USIDR = out;
    byte v1 = bit(USIWM0) | bit(USITC);
    byte v2 = bit(USIWM0) | bit(USITC) | bit(USICLK);
#if F_CPU <= 5000000
    // only unroll if resulting clock stays under 2.5 MHz
    USICR = v1; USICR = v2;
    USICR = v1; USICR = v2;
    USICR = v1; USICR = v2;
    USICR = v1; USICR = v2;
    USICR = v1; USICR = v2;
    USICR = v1; USICR = v2;
    USICR = v1; USICR = v2;
    USICR = v1; USICR = v2;
#else
    for (uint8_t i = 0; i < 8; ++i) {
        USICR = v1;
        USICR = v2;
    }
#endif
    return USIDR;
#endif
}

static uint16_t rf12_xfer (uint16_t cmd) {
    bitClear(SS_PORT, SS_BIT);
    uint16_t reply = rf12_byte(cmd >> 8) << 8;
    reply |= rf12_byte(cmd);
    bitSet(SS_PORT, SS_BIT);
    return reply;
}

// access to the RFM12B internal registers with interrupts disabled
uint16_t rf12_control(uint16_t cmd) {
#ifdef EIMSK
    bitClear(EIMSK, INT0);
    uint16_t r = rf12_xfer(cmd);
    bitSet(EIMSK, INT0);
#else
    // ATtiny
    bitClear(GIMSK, INT0);
    uint16_t r = rf12_xfer(cmd);
    bitSet(GIMSK, INT0);
#endif
    return r;
}

static void rf12_interrupt() 
{
bitSet(PINB, 0);
    // a transfer of 2x 16 bits @ 2 MHz over SPI takes 2x 8 us inside this ISR
    rf12_xfer(0x0000);
    
	/* DDL : receive */
    if (rxstate == TXRECV) 
	{
        uint8_t in = rf12_xfer(RF_RX_FIFO_READ); // DDL: read FIFO buffer
		
		if((++count) == 2 ) 
		{
		
			RSSI = analogRead(A0);
		} // DDL: sampling RSSI when count == 2 
/* DDL: get the maximum value of RSSI or average?
 * reserved:
 */
		if (rxfill == 0 && group != 0)
            rf12_buf[rxfill++] = group;
            
        rf12_buf[rxfill++] = in;
        rf12_crc = _crc16_update(rf12_crc, in);

        if (rxfill >= rf12_len + 5 || rxfill >= RF_MAX)
            rf12_xfer(RF_IDLE_MODE);
    } 
	/* DDL: send */
	else 
	{
        uint8_t out;

        if (rxstate < 0) 
		{
            uint8_t pos = 3 + rf12_len + rxstate++;
            out = rf12_buf[pos];
            rf12_crc = _crc16_update(rf12_crc, out);
        } 
		else
            switch (rxstate++) 
			{
                case TXSYN1: out = 0x2D; break;
                case TXSYN2: out = rf12_grp; rxstate = - (2 + rf12_len); break;
                case TXCRC1: out = rf12_crc; break;
                case TXCRC2: out = rf12_crc >> 8; break;
                case TXDONE: rf12_xfer(RF_IDLE_MODE); // fall through
                default:     out = 0xAA;
            }
            
        rf12_xfer(RF_TXREG_WRITE + out);
    }
// bitClear(PINB, 0);
}

static void rf12_recvStart () {
    rxfill = rf12_len = 0;
    rf12_crc = ~0;
#if RF12_VERSION >= 2
    if (group != 0)
        rf12_crc = _crc16_update(~0, group);
#endif
    rxstate = TXRECV;    
    rf12_xfer(RF_RECEIVER_ON);
}

uint8_t rf12_recvDone () {
    if (rxstate == TXRECV && (rxfill >= rf12_len + 5 || rxfill >= RF_MAX)) {
        rxstate = TXIDLE;
        if (rf12_len > RF12_MAXDATA)
            rf12_crc = 1; // force bad crc if packet length is invalid
        if (!(rf12_hdr & RF12_HDR_DST) ||
                (rf12_hdr & RF12_HDR_MASK) == (nodeid & NODE_ID)) {
            if (rf12_crc == 0 && crypter != 0)
                crypter(0);
            else
                rf12_seq = -1;
			
			/* DDL: alculate avg RSSI */
			//int temp = count/8 + 1;
			//RSSI = RSSI/temp;
			count = 0;
						
            return 1; // it's a broadcast packet or it's addressed to this node
        }
    }
    if (rxstate == TXIDLE)
        rf12_recvStart();
    return 0;
}

uint8_t rf12_canSend () {
    // no need to test with interrupts disabled: state TXRECV is only reached
    // outside of ISR and we don't care if rxfill jumps from 0 to 1 here
    if (rxstate == TXRECV && rxfill == 0 &&
            (rf12_byte(0x00) & (RF_RSSI_BIT >> 8)) == 0) {
        rf12_xfer(RF_IDLE_MODE); // stop receiver
        //XXX just in case, don't know whether these RF12 reads are needed!
        // rf12_xfer(0x0000); // status register
        // rf12_xfer(RF_RX_FIFO_READ); // fifo read
        rxstate = TXIDLE;
        rf12_grp = group;
        return 1;
    }
    return 0;
}

void rf12_sendStart (uint8_t hdr) {
    rf12_hdr = hdr & RF12_HDR_DST ? hdr :
                (hdr & ~RF12_HDR_MASK) + (nodeid & NODE_ID);
    if (crypter != 0)
        crypter(1);
    
    rf12_crc = ~0;
#if RF12_VERSION >= 2
    rf12_crc = _crc16_update(rf12_crc, rf12_grp);
#endif
    rxstate = TXPRE1;
    rf12_xfer(RF_XMITTER_ON); // bytes will be fed via interrupts
}

void rf12_sendStart (uint8_t hdr, const void* ptr, uint8_t len) {
    rf12_len = len;
    memcpy((void*) rf12_data, ptr, len);
    rf12_sendStart(hdr);
}

// deprecated
void rf12_sendStart (uint8_t hdr, const void* ptr, uint8_t len, uint8_t sync) {
    rf12_sendStart(hdr, ptr, len);
    rf12_sendWait(sync);
}

void rf12_sendWait (uint8_t mode) {
    // wait for packet to actually finish sending
    // go into low power mode, as interrupts are going to come in very soon
    while (rxstate != TXIDLE)
        if (mode) {
            // power down mode is only possible if the fuses are set to start
            // up in 258 clock cycles, i.e. approx 4 us - else must use standby!
            // modes 2 and higher may lose a few clock timer ticks
            set_sleep_mode(mode == 3 ? SLEEP_MODE_PWR_DOWN :
#ifdef SLEEP_MODE_STANDBY
                           mode == 2 ? SLEEP_MODE_STANDBY :
#endif
                                       SLEEP_MODE_IDLE);
            sleep_mode();
        }
}

/*!
  Call this once with the node ID (0-31), frequency band (0-3), and
  optional group (0-255 for RF12B, only 212 allowed for RF12).
*/
void rf12_initialize (uint8_t id, uint8_t band, uint8_t g) {
    nodeid = id;
    group = g;
    
    spi_initialize();
    
    pinMode(RFM_IRQ, INPUT);
    digitalWrite(RFM_IRQ, 1); // pull-up

    rf12_xfer(0x0000); // intitial SPI transfer added to avoid power-up problem

    rf12_xfer(RF_SLEEP_MODE); // DC (disable clk pin), enable lbd
    
    // wait until RFM12B is out of power-up reset, this takes several *seconds*
    rf12_xfer(RF_TXREG_WRITE); // in case we're still in OOK mode
    while (digitalRead(RFM_IRQ) == 0)
        rf12_xfer(0x0000);
        
    rf12_xfer(0x80C7 | (band << 4)); // EL (ena TX), EF (ena RX FIFO), 12.0pF 
    rf12_xfer(0xA640); // 868MHz 
    rf12_xfer(0xC606); // approx 49.2 Kbps, i.e. 10000/29/(1+6) Kbps
    rf12_xfer(0x94A2); // VDI,FAST,134kHz,0dBm,-91dBm 
    rf12_xfer(0xC2AC); // AL,!ml,DIG,DQD4 
    if (group != 0) {
        rf12_xfer(0xCA83); // FIFO8,2-SYNC,!ff,DR 
        rf12_xfer(0xCE00 | group); // SYNC=2DXX； 
    } else {
        rf12_xfer(0xCA8B); // FIFO8,1-SYNC,!ff,DR 
        rf12_xfer(0xCE2D); // SYNC=2D； 
    }
    rf12_xfer(0xC483); // @PWR,NO RSTRIC,!st,!fi,OE,EN 
    rf12_xfer(0x9857); // -21db
	//rf12_xfer(0x9850); // !mp,90kHz,MAX OUT
	//rf12_xfer(0x9850); // !mp,90kHz,MAX OUT
	//rf12_xfer(0x9850); // !mp,90kHz,MAX OUT
	//rf12_xfer(0x9850); // !mp,90kHz,MAX OUT
	//rf12_xfer(0x9850); // !mp,90kHz,MAX OUT	
    rf12_xfer(0xCC77); // OB1，OB0, LPX,！ddy，DDIT，BW0 
    rf12_xfer(0xE000); // NOT USE 
    rf12_xfer(0xC800); // NOT USE 
    rf12_xfer(0xC049); // 1.66MHz,3.1V 

    rxstate = TXIDLE;
    if ((nodeid & NODE_ID) != 0)
        attachInterrupt(0, rf12_interrupt, LOW);
    else
        detachInterrupt(0);
}

void rf12_onOff (uint8_t value) {
    rf12_xfer(value ? RF_XMITTER_ON : RF_IDLE_MODE);
}

uint8_t rf12_config () {
    uint16_t crc = ~0;
    for (uint8_t i = 0; i < RF12_EEPROM_SIZE; ++i)
        crc = _crc16_update(crc, eeprom_read_byte(RF12_EEPROM_ADDR + i));
    if (crc != 0)
        return 0;
        
    uint8_t nodeId = 0, group = 0;
    for (uint8_t i = 0; i < RF12_EEPROM_SIZE - 2; ++i) {
        uint8_t b = eeprom_read_byte(RF12_EEPROM_ADDR + i);
        if (i == 0)
            nodeId = b;
        else if (i == 1)
            group = b;
        else if (b == 0)
            break;
        else
            Serial.print(b);
    }
    Serial.println();
    
    rf12_initialize(nodeId, nodeId >> 6, group);
    return nodeId & RF12_HDR_MASK;
}

void rf12_sleep (char n) {
    if (n < 0)
        rf12_control(RF_IDLE_MODE);
    else {
        rf12_control(RF_WAKEUP_TIMER | 0x0500 | n);
        rf12_control(RF_SLEEP_MODE);
        if (n > 0)
            rf12_control(RF_WAKEUP_MODE);
    }
    rxstate = TXIDLE;
}

char rf12_lowBat () {
    return (rf12_control(0x0000) & RF_LBD_BIT) != 0;
}

void rf12_easyInit (uint8_t secs) {
    ezInterval = secs;
}

char rf12_easyPoll () {
    if (rf12_recvDone() && rf12_crc == 0) {
        byte myAddr = nodeid & RF12_HDR_MASK;
        if (rf12_hdr == (RF12_HDR_CTL | RF12_HDR_DST | myAddr)) {
            ezPending = 0;
            ezNextSend[0] = 0; // flags succesful packet send
            if (rf12_len > 0)
                return 1;
        }
    }
    if (ezPending > 0) {
        // new data sends should not happen less than ezInterval seconds apart
        // ... whereas retries should not happen less than RETRY_MS apart
        byte newData = ezPending == RETRIES;
        long now = millis();
        if (now >= ezNextSend[newData] && rf12_canSend()) {
            ezNextSend[0] = now + RETRY_MS;
            // must send new data packets at least ezInterval seconds apart
            // ezInterval == 0 is a special case:
            //      for the 868 MHz band: enforce 1% max bandwidth constraint
            //      for other bands: use 100 msec, i.e. max 10 packets/second
            if (newData)
                ezNextSend[1] = now +
                    (ezInterval > 0 ? 1000L * ezInterval
                                    : (nodeid >> 6) == RF12_868MHZ ?
                                            13 * (ezSendLen + 10) : 100);
            rf12_sendStart(RF12_HDR_ACK, ezSendBuf, ezSendLen);
            --ezPending;
        }
    }
    return ezPending ? -1 : 0;
}

char rf12_easySend (const void* data, uint8_t size) {
    if (data != 0 && size != 0) {
        if (ezNextSend[0] == 0 && size == ezSendLen &&
                                    memcmp(ezSendBuf, data, size) == 0)
            return 0;
        memcpy(ezSendBuf, data, size);
        ezSendLen = size;
    }
    ezPending = RETRIES;
    return 1;
}

// XXTEA by David Wheeler, adapted from http://en.wikipedia.org/wiki/XXTEA

#define DELTA 0x9E3779B9
#define MX (((z>>5^y<<2) + (y>>3^z<<4)) ^ ((sum^y) + \
                                            (cryptKey[(uint8_t)((p&3)^e)] ^ z)))

static void cryptFun (uint8_t send) {
    uint32_t y, z, sum, *v = (uint32_t*) rf12_data;
    uint8_t p, e, rounds = 6;
    
    if (send) {
        // pad with 1..4-byte sequence number
        *(uint32_t*)(rf12_data + rf12_len) = ++seqNum;
        uint8_t pad = 3 - (rf12_len & 3);
        rf12_len += pad;
        rf12_data[rf12_len] &= 0x3F;
        rf12_data[rf12_len] |= pad << 6;
        ++rf12_len;
        // actual encoding
        char n = rf12_len / 4;
        if (n > 1) {
            sum = 0;
            z = v[n-1];
            do {
                sum += DELTA;
                e = (sum >> 2) & 3;
                for (p=0; p<n-1; p++)
                    y = v[p+1], z = v[p] += MX;
                y = v[0];
                z = v[n-1] += MX;
            } while (--rounds);
        }
    } else if (rf12_crc == 0) {
        // actual decoding
        char n = rf12_len / 4;
        if (n > 1) {
            sum = rounds*DELTA;
            y = v[0];
            do {
                e = (sum >> 2) & 3;
                for (p=n-1; p>0; p--)
                    z = v[p-1], y = v[p] -= MX;
                z = v[n-1];
                y = v[0] -= MX;
            } while ((sum -= DELTA) != 0);
        }
        // strip sequence number from the end again
        if (n > 0) {
            uint8_t pad = rf12_data[--rf12_len] >> 6;
            rf12_seq = rf12_data[rf12_len] & 0x3F;
            while (pad-- > 0)
                rf12_seq = (rf12_seq << 8) | rf12_data[--rf12_len];
        }
    }
}

void rf12_encrypt (const uint8_t* key) {
    // by using a pointer to cryptFun, we only link it in when actually used
    if (key != 0) {
        for (uint8_t i = 0; i < sizeof cryptKey; ++i)
            ((uint8_t*) cryptKey)[i] = eeprom_read_byte(key + i);
        crypter = cryptFun;
    } else
        crypter = 0;
}

// DDL: get recorded RSSI
int readRSSI()
{
	return RSSI;
}
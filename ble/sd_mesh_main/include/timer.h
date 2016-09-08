#ifndef __TIMER_H__
#define __TIMER_H__

#include <ble.h>
#include <stdbool.h>
#include "app_timer.h"

// TIMER SETTINGS
#define APP_TIMER_PRESCALER             15    // Value of the RTC1 PRESCALER register.
#define APP_TIMER_OP_QUEUE_SIZE         5     // Size of timer operation queues.
#define APP_MAX_TIMER_NUMBER         		5     // Maximum number of simultaneously active timers

static app_timer_id_t m_timer_debounce_id;
void msd_timer_init(void);
void setLastTickActive(bool value);
void setCurrentTickActive(bool value);
void setTimerActive(bool value);
bool getTimerActive(void);
void stopDebounceTimer(void);
void stopBlinkTimer(void);
void startBlinkTimerIfNeccessary(void);
	
#endif 

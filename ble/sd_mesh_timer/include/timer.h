#ifndef __TIMER_H__
#define __TIMER_H__

#include <ble.h>
#include <stdbool.h>
#include "app_timer.h"

// TIMER SETTINGS
#define APP_TIMER_PRESCALER             15    // Value of the RTC1 PRESCALER register.
#define APP_TIMER_OP_QUEUE_SIZE         6     // Size of timer operation queues.
#define APP_MAX_TIMER_NUMBER         		6     // Maximum number of simultaneously active timers

static bool isCurrentTickActive = false;
static bool isLastTickActive = false; 
static bool isTimerActive = false;
static app_timer_id_t m_timer_debounce_id;
static app_timer_id_t m_timer_toggle_alarm_id;
void msd_timer_init(void);
void setLastTickActive(bool value);
void stopDebounceTimer(void);
void stopBlinkTimer(void);
void toggleAlarmTimer(void);
void startBlinkTimerIfNeccessary(void);
	
#endif 

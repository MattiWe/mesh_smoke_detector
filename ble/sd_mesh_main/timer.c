#include "app_error.h"
#include "io_config.h"
#include "softdevice_handler.h"
#include "mesh_gatt.h"
#include "nrf.h"
#include "nrf_gpio.h"

#include "rbc_mesh.h"
#include "timer.h"
#include "io_config.h"
#include "nrf_drv_clock.h"
#include "nrf_adv_conn.h"

#include <stdint.h>
#include <string.h>

bool isCurrentTickActive = false;
bool isLastTickActive = false; 
bool isTimerActive = false;
static app_timer_id_t m_timer_blink_id;

static void debounce_timeout_handler(void *p_context){			// handle what happens if timer runs out
		if(isLastTickActive && !isCurrentTickActive){ 					// first tick
				isLastTickActive = false;
				isCurrentTickActive = true;
		}else if(isLastTickActive && isCurrentTickActive){ 			// Alarm active, set mesh to 1
				isLastTickActive = false;
				setMeshValueTo(1,1); 																// set Mesh value to 1
				nrf_gpio_pin_set(VALUE_LED_1);
				uint8_t data[7] = {0x01}; // TODO
				change_adv_manu_data(data);
		}else if(!isLastTickActive && isCurrentTickActive){ 		// alarm stoped / standby Signal, set mesh to 0	
			stopDebounceTimer();
		}
		
}

static void blink_timeout_handler(void *p_context){
	nrf_gpio_pin_toggle(STATUS_LED);
}

void msd_timer_init(void){
		uint32_t error_code;
		// Initialize the application timer module.
    APP_TIMER_INIT(APP_TIMER_PRESCALER, APP_MAX_TIMER_NUMBER, APP_TIMER_OP_QUEUE_SIZE, false);
		error_code = app_timer_create(&m_timer_debounce_id,
                                APP_TIMER_MODE_REPEATED,
                                debounce_timeout_handler);
    APP_ERROR_CHECK(error_code);
	
	  error_code = app_timer_create(&m_timer_blink_id,
                                APP_TIMER_MODE_REPEATED,
                                blink_timeout_handler);
    APP_ERROR_CHECK(error_code);
}

void stopDebounceTimer(){
		uint32_t errorCode;
		nrf_gpio_pin_clear(VALUE_LED_1); 										// turn off LED		
		errorCode = app_timer_stop(m_timer_debounce_id); 		// stop timer	
		APP_ERROR_CHECK(errorCode);
		setMeshValueTo(1, 0);																// set mesh value to 0
		isCurrentTickActive = false;
		isTimerActive = false;	
		isLastTickActive = false;
		uint8_t data[7] = {0x00}; // TODO
		change_adv_manu_data(data);
}

void setLastTickActive(bool value){
	isLastTickActive = value;
}

void setCurrentTickActive(bool value){
	isCurrentTickActive = value;
}
	
void setTimerActive(bool value){
	isTimerActive = value;
}

void stopBlinkTimer(){	
		uint32_t errorCode;
		errorCode = app_timer_stop(m_timer_blink_id); 		// stop timer	
		APP_ERROR_CHECK(errorCode);
	
}

bool getTimerActive(){
	return isTimerActive;
}

// blink LED when advertising
void startBlinkTimerIfNeccessary(){
		uint32_t 		errorCode;
		errorCode = app_timer_start(m_timer_blink_id, 
					APP_TIMER_TICKS(1000, APP_TIMER_PRESCALER), 
					NULL);
		APP_ERROR_CHECK(errorCode);
}

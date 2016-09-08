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

static app_timer_id_t m_timer_blink_id;
bool isAlarmTimerActive = false;

static void blink_timeout_handler(void *p_context){
	nrf_gpio_pin_toggle(STATUS_LED);
}

static void toggle_alarm_timeout_handler(void *p_context){
	  uint8_t mesh_data[2] = {0,0};
	  uint16_t	  len = 2;
		uint32_t 		errorCode;
		errorCode = rbc_mesh_value_get(1, mesh_data, &len);
		APP_ERROR_CHECK(errorCode);
		if(mesh_data[0] == 0){
				setMeshValueTo(1,1); 
				nrf_gpio_pin_set(VALUE_LED_1);	
				uint8_t data[7] = {0x01};
				change_adv_manu_data(data);		
		}else if(mesh_data[0] == 1){
				setMeshValueTo(1,0); 	
				nrf_gpio_pin_clear(VALUE_LED_1);
				uint8_t data[7] = {0x00};
				change_adv_manu_data(data);			
		}
		APP_ERROR_CHECK(errorCode);
}

void msd_timer_init(void){
		uint32_t error_code;
		// Initialize the application timer module.
    APP_TIMER_INIT(APP_TIMER_PRESCALER, APP_MAX_TIMER_NUMBER, APP_TIMER_OP_QUEUE_SIZE, false);
	
	  error_code = app_timer_create(&m_timer_blink_id,
                                APP_TIMER_MODE_REPEATED,
                                blink_timeout_handler);
    APP_ERROR_CHECK(error_code);
	  error_code = app_timer_create(&m_timer_toggle_alarm_id,
                                APP_TIMER_MODE_REPEATED,
                                toggle_alarm_timeout_handler);
    APP_ERROR_CHECK(error_code);
}

void stopBlinkTimer(){	
		uint32_t errorCode;
		errorCode = app_timer_stop(m_timer_blink_id); 		// stop timer	
		APP_ERROR_CHECK(errorCode);
}

void startBlinkTimerIfNeccessary(){
		uint32_t 		errorCode;
		errorCode = app_timer_start(m_timer_blink_id, 
					APP_TIMER_TICKS(1000, APP_TIMER_PRESCALER), 
					NULL);
		APP_ERROR_CHECK(errorCode);
}

void toggleAlarmTimer(){
		uint32_t 		errorCode;
		if(!isAlarmTimerActive){
			errorCode = app_timer_start(m_timer_toggle_alarm_id, 
						APP_TIMER_TICKS(10000, APP_TIMER_PRESCALER), 
						NULL);
			isAlarmTimerActive = true;
		/*}else{
			errorCode = app_timer_stop(m_timer_toggle_alarm_id);
			isAlarmTimerActive = false;
			nrf_gpio_pin_clear(VALUE_LED_1);
		*/}
		APP_ERROR_CHECK(errorCode);
}

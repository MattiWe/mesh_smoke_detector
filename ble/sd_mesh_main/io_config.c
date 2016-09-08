#include "app_error.h"
#include "nrf_gpio.h"
#include "io_config.h"
#include "nrf_drv_gpiote.h"
#include "softdevice_handler.h"
#include "mesh_gatt.h"
#include "nrf.h"

#include "rbc_mesh.h"
#include "app_timer.h"
#include "timer.h"
#include "nrf_drv_clock.h"
#include "nrf_adv_conn.h"

#include <stdint.h>
#include <string.h>
#include <stdbool.h>

static uint8_t mesh_data[16] = {0,0};
static uint16_t	  len = 2;

void setMeshValueTo(uint8_t handle, uint8_t newValue){
		uint32_t 		errorCode;
		errorCode = rbc_mesh_value_get(handle, mesh_data, &len);
		if(mesh_data[0] != newValue){
				mesh_data[0] = newValue;
				errorCode = rbc_mesh_value_set(handle, mesh_data, 2);
				APP_ERROR_CHECK(errorCode);
				// Change Chracteristic Value: mesh_gatt_update_data_char_value(mesh_data);
		}		
	
}

void in_pin_handler(nrf_drv_gpiote_pin_t pin, nrf_gpiote_polarity_t action)
{
		uint32_t 		error_code;
		if(pin == RESET_BUTTON){
			//nrf_gpio_pin_set(VALUE_LED_1);
			setLastTickActive(true);
			//error_code = rbc_mesh_value_get(1, mesh_data, &len);		
			//if(error_code == NRF_SUCCESS){
			if(!getTimerActive()){
				// set DETECTOR_ACTIVE value to 1
				// start timer with 1500 ms - 1.5 sec
				error_code = app_timer_start(
						m_timer_debounce_id, 
						APP_TIMER_TICKS(1500, APP_TIMER_PRESCALER), 
						NULL);
				APP_ERROR_CHECK(error_code);
				setTimerActive(true);
			}
		}
}
/**
 * @brief Function for configuring: PIN_IN pin for input, PIN_OUT pin for output, 
 * and configures GPIOTE to give an interrupt on pin change.
 */
void gpio_init(void)
{
    uint32_t err_code;

    err_code = nrf_drv_gpiote_init();
    APP_ERROR_CHECK(err_code);
    
    nrf_drv_gpiote_out_config_t out_config = GPIOTE_CONFIG_OUT_SIMPLE(false);

    err_code = nrf_drv_gpiote_out_init(VALUE_LED_1, &out_config);
    APP_ERROR_CHECK(err_code);
		err_code = nrf_drv_gpiote_out_init(STATUS_LED, &out_config);
    APP_ERROR_CHECK(err_code);

    nrf_drv_gpiote_in_config_t in_config = GPIOTE_CONFIG_IN_SENSE_TOGGLE(true);
    in_config.pull = NRF_GPIO_PIN_PULLDOWN;

    err_code = nrf_drv_gpiote_in_init(RESET_BUTTON, &in_config, in_pin_handler);
    APP_ERROR_CHECK(err_code);

    nrf_drv_gpiote_in_event_enable(RESET_BUTTON, true);		
}


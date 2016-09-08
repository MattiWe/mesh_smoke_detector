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

void setMeshValueTo(uint8_t handle, uint8_t newValue){
		uint32_t 		errorCode;
		uint8_t mesh_data[2] = {0,0};
		uint16_t	  len = 2;
		errorCode = rbc_mesh_value_get(handle, mesh_data, &len);
		if(mesh_data[0] != newValue){
				mesh_data[0] = newValue;
				errorCode = rbc_mesh_value_set(handle, mesh_data, 2);
				APP_ERROR_CHECK(errorCode);
		}			
}

void in_pin_handler(nrf_drv_gpiote_pin_t pin, nrf_gpiote_polarity_t action)
{
		if(pin == RESET_BUTTON){
			toggleAlarmTimer();
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


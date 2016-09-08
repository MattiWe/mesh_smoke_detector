/***********************************************************************************
Copyright (c) Nordic Semiconductor ASA
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright notice, this
  list of conditions and the following disclaimer in the documentation and/or
  other materials provided with the distribution.

  3. Neither the name of Nordic Semiconductor ASA nor the names of other
  contributors to this software may be used to endorse or promote products
  derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
************************************************************************************/

#include "rbc_mesh.h"
#include "bootloader_app.h"

#include "nrf_adv_conn.h"
#include "mesh_aci.h"
#include "mesh_gatt.h"

#include "softdevice_handler.h"
#include "app_error.h"
#include "nrf_gpio.h"
#include "io_config.h"
#include "boards.h"
#include "app_timer.h"
#include "nrf_drv_clock.h"
#include "timer.h"
#include <stdbool.h>
#include <stdint.h>
#include <string.h>
#include <stdio.h>

/* Debug macros for debugging with logic analyzer */
#define SET_PIN(x) NRF_GPIO->OUTSET = (1 << (x))
#define CLEAR_PIN(x) NRF_GPIO->OUTCLR = (1 << (x))
#define TICK_PIN(x) do { SET_PIN((x)); CLEAR_PIN((x)); }while(0)

#define MESH_ACCESS_ADDR        (RBC_MESH_ACCESS_ADDRESS_BLE_ADV)
#define MESH_INTERVAL_MIN_MS    (100)
#define MESH_CHANNEL            (38)
#define MESH_CLOCK_SOURCE       (NRF_CLOCK_LFCLKSRC_XTAL_75_PPM)

/**
* @brief General error handler.
*/
static void error_loop(void)
{	
    nrf_gpio_pin_clear(STATUS_LED);
    nrf_gpio_pin_clear(VALUE_LED_1);
    while (true)
    {
			__WFE();
    }
}

/**
* @brief Softdevice crash handler, never returns
*
* @param[in] pc Program counter at which the assert failed
* @param[in] line_num Line where the error check failed
* @param[in] p_file_name File where the error check failed
*/
void sd_assert_handler(uint32_t pc, uint16_t line_num, const uint8_t* p_file_name)
{
    error_loop();
}

/**
* @brief App error handle callback. Called whenever an APP_ERROR_CHECK() fails.
*   Never returns.
*
* @param[in] error_code The error code sent to APP_ERROR_CHECK()
* @param[in] line_num Line where the error check failed
* @param[in] p_file_name File where the error check failed
*/
void app_error_handler(uint32_t error_code, uint32_t line_num, const uint8_t * p_file_name)
{
	  error_loop();
}

void HardFault_Handler(void)
{
    error_loop();
}

/**
* @brief Softdevice event handler
*/
void sd_ble_evt_handler(ble_evt_t* p_ble_evt)
{
    rbc_mesh_ble_evt_handler(p_ble_evt);
    nrf_adv_conn_evt_handler(p_ble_evt);
}
/**
* @brief RBC_MESH framework event handler. Defined in rbc_mesh.h. Handles
*   events coming from the mesh. Sets LEDs according to data
*
* @param[in] evt RBC event propagated from framework
*/
static void rbc_mesh_event_handler(rbc_mesh_event_t* evt)
{
    switch (evt->event_type)
    {
        case RBC_MESH_EVENT_TYPE_CONFLICTING_VAL:
        case RBC_MESH_EVENT_TYPE_NEW_VAL:				
        case RBC_MESH_EVENT_TYPE_UPDATE_VAL:
            if (evt->value_handle == 1){
							// TODO change advertising
							if(evt->data[0] == 1){
								nrf_gpio_pin_set(VALUE_LED_1);
								uint8_t data[7] = {0x01}; // TODO
								change_adv_manu_data(data);
							}
							if(evt->data[0] == 0){
								stopDebounceTimer();
							}
						}				
						break;
        case RBC_MESH_EVENT_TYPE_TX:
            break;
        case RBC_MESH_EVENT_TYPE_INITIALIZED:
            /* init BLE gateway softdevice application: */
            //nrf_adv_conn_init();
            break;
    }
}

/** @brief main function */
int main(void)
{
    /* Enable Softdevice (including sd_ble before framework */
    SOFTDEVICE_HANDLER_INIT(MESH_CLOCK_SOURCE, NULL);
    softdevice_ble_evt_handler_set(sd_ble_evt_handler); /* app-defined event handler, as we need to send it to the nrf_adv_conn module and the rbc_mesh */
    softdevice_sys_evt_handler_set(rbc_mesh_sd_evt_handler);
	
		uint32_t error_code;

#ifdef RBC_MESH_SERIAL

    /* only want to enable serial interface, and let external host setup the framework */
    mesh_aci_init();

#else

    rbc_mesh_init_params_t init_params;

    init_params.access_addr = MESH_ACCESS_ADDR;
    init_params.interval_min_ms = MESH_INTERVAL_MIN_MS;
    init_params.channel = MESH_CHANNEL;
    init_params.lfclksrc = MESH_CLOCK_SOURCE;

    error_code = rbc_mesh_init(init_params);
    APP_ERROR_CHECK(error_code);	
		
    uint8_t mesh_data[2] = {0,0};
		rbc_mesh_value_set(1, mesh_data, 2);
		
		msd_timer_init();
		
		/* init leds and pins */
    gpio_init();
		
		/* init Advertising */
    nrf_adv_conn_init();

#endif

		
    rbc_mesh_event_t evt;
    while (true)
    {
        if (rbc_mesh_event_get(&evt) == NRF_SUCCESS)
        {
            rbc_mesh_event_handler(&evt);
            rbc_mesh_packet_release(evt.data);
        }
        sd_app_evt_wait();
    }


}

